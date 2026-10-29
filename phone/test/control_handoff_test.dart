import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show FakeBridge, snapshot;

Json proposal({bool available = true, bool authorized = false}) =>
    snapshot(authorized: authorized)
      ..['controlRequestAvailable'] = available
      ..['status'] = 'Another approved controller has control. Open Setup.';

void main() {
  test('only a fresh native proposal enables one deliberate handoff', () async {
    final bridge = FakeBridge();
    final remote = Remote(bridge);
    remote.accept(snapshot(authorized: false));
    await remote.requestControl();
    expect(bridge.calls, isEmpty);
    remote.accept(proposal(authorized: true));
    await remote.requestControl();
    expect(bridge.calls, isEmpty);
    remote.accept(proposal());
    bridge.pending = Completer<dynamic>();
    final first = remote.requestControl();
    await remote.requestControl();
    expect(bridge.calls.map((e) => e.$1), ['requestControl']);
    expect(remote.canRequestControl, isFalse);
    bridge.pending!.complete();
    await first;
    await remote.requestControl();
    expect(bridge.calls, hasLength(1));
    remote.accept(proposal(available: false, authorized: true));
    expect(remote.action('canPlay'), isTrue);
    expect(bridge.calls, hasLength(1)); // Grant never starts playback.
    remote.dispose();
  });

  test(
    'failed handoff never retries or restores consumed availability',
    () async {
      final bridge = FakeBridge()..pending = Completer<dynamic>();
      final remote = Remote(bridge)..accept(proposal());
      final request = remote.requestControl();
      bridge.pending!.completeError(StateError('timeout'));
      await request;
      await remote.requestControl();
      expect(remote.canRequestControl, isFalse);
      expect(remote.error, contains('interrupted'));
      expect(bridge.calls, hasLength(1));
      remote.accept(snapshot(authorized: false));
      expect(remote.action('canPlay'), isFalse);
      remote.accept({'bridgeVersion': 99});
      expect(remote.canRequestControl, isFalse);
      remote.dispose();
    },
  );

  for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
    testWidgets('$platform handoff wraps at large text and stays deliberate', (
      tester,
    ) async {
      debugDefaultTargetPlatformOverride = platform;
      addTearDown(() => debugDefaultTargetPlatformOverride = null);
      tester.view.physicalSize = const Size(320, 720);
      tester.view.devicePixelRatio = 1;
      tester.platformDispatcher.textScaleFactorTestValue = 2.5;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
      final bridge = FakeBridge();
      final remote = Remote(bridge);
      await tester.pumpWidget(DX340App(remote: remote));
      await tester.pumpAndSettle();
      remote.accept(proposal());
      await tester.tap(find.text('Setup').last);
      await tester.pumpAndSettle();
      final takeControl = find.text('Take control on this phone');
      await tester.scrollUntilVisible(takeControl, 160);
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      expect(tester.getBottomRight(takeControl).dx, lessThanOrEqualTo(320));
      await tester.tap(takeControl);
      await tester.pumpAndSettle();
      expect(bridge.calls.where((e) => e.$1 == 'requestControl'), hasLength(1));
      expect(bridge.calls.where((e) => e.$1 == 'send'), isEmpty);
      remote.accept(snapshot(authorized: false)..['status'] = 'Control moved.');
      await tester.pumpAndSettle();
      expect(takeControl, findsNothing);
      expect(remote.action('canPlay'), isFalse);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
      debugDefaultTargetPlatformOverride = null;
    });
  }
}
