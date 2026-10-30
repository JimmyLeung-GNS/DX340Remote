import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show snapshot;

class ConnectionBridge implements RemoteBridge {
  final stream = StreamController<dynamic>.broadcast(sync: true);
  final calls = <String>[];
  Completer<bool>? permission;
  bool allowed = true;
  @override
  Stream<dynamic> get events => stream.stream;
  @override
  Future<dynamic> call(String method, [Json args = const {}]) async {
    calls.add(method);
    if (method == 'snapshot') return snapshot(authorized: false);
    if (method == 'permissions') return permission?.future ?? allowed;
    return null;
  }
}

Json setupSnapshot(String phase) => snapshot(authorized: phase == 'connected')
  ..['connectionSetup'] = {
    'phase': phase,
    'hasSavedPlayer': true,
    'savedName': 'DX340',
  };

void main() {
  for (final brightness in Brightness.values) {
    for (final highContrast in [false, true]) {
      testWidgets(
        'iPhone Setup action contrast $brightness highContrast=$highContrast stays scoped',
        (tester) async {
          debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
          addTearDown(() => debugDefaultTargetPlatformOverride = null);
          tester.view.physicalSize = const Size(390, 844);
          tester.view.devicePixelRatio = 1;
          tester.platformDispatcher.platformBrightnessTestValue = brightness;
          tester.platformDispatcher.accessibilityFeaturesTestValue =
              FakeAccessibilityFeatures(highContrast: highContrast);
          addTearDown(tester.view.resetPhysicalSize);
          addTearDown(tester.view.resetDevicePixelRatio);
          addTearDown(
            tester.platformDispatcher.clearPlatformBrightnessTestValue,
          );
          addTearDown(
            tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
          );
          await tester.pumpWidget(DX340App(remote: Remote(ConnectionBridge())));
          await tester.pumpAndSettle();
          final original = CupertinoTheme.of(
            tester.element(find.text('Open Setup')),
          ).primaryColor;
          await tester.tap(find.text('Setup').last);
          await tester.pumpAndSettle();
          final theme = CupertinoTheme.of(tester.element(find.text('Connect')));
          double contrast(Color a, Color b) {
            final x = a.computeLuminance(), y = b.computeLuminance();
            return x > y ? (x + .05) / (y + .05) : (y + .05) / (x + .05);
          }

          expect(
            contrast(theme.primaryColor, theme.primaryContrastingColor),
            greaterThanOrEqualTo(4.5),
          );
          expect(
            contrast(theme.primaryColor, theme.scaffoldBackgroundColor),
            greaterThanOrEqualTo(4.5),
          );
          await tester.tap(find.text('Player').last);
          await tester.pumpAndSettle();
          expect(
            CupertinoTheme.of(
              tester.element(find.text('Open Setup')),
            ).primaryColor,
            original,
          );
          await tester.pumpWidget(const SizedBox.shrink());
          debugDefaultTargetPlatformOverride = null;
        },
      );
    }
  }

  test(
    'one Connect requests permission and connects once; never sends playback',
    () async {
      final bridge = ConnectionBridge()..permission = Completer<bool>();
      final remote = Remote(bridge);
      final first = remote.connectPlayer(apple: false);
      await remote.connectPlayer(apple: false);
      expect(bridge.calls, ['permissions']);
      bridge.permission!.complete(true);
      await first;
      expect(bridge.calls, ['permissions', 'connectPlayer']);
      remote.accept(setupSnapshot('approval'));
      await remote.connectPlayer(apple: false);
      expect(bridge.calls, hasLength(2));
      remote.dispose();
    },
  );

  test('denial does not connect or treat permission as a grant', () async {
    final bridge = ConnectionBridge()..allowed = false;
    final remote = Remote(bridge);
    await remote.connectPlayer(apple: false);
    expect(bridge.calls, ['permissions']);
    expect(remote.connectionPhase, 'permission');
    expect(remote.authorized, isFalse);
    remote.dispose();
  });

  for (final failedBridge in [false, true]) {
    test(
      'late permission result cannot connect after ${failedBridge ? 'bridge failure' : 'Cancel'}',
      () async {
        final bridge = ConnectionBridge()..permission = Completer<bool>();
        final remote = Remote(bridge);
        final first = remote.connectPlayer(apple: false);
        if (failedBridge) {
          remote.accept({'bridgeVersion': 99});
        } else {
          await remote.cancelConnection();
        }
        bridge.permission!.complete(true);
        await first;
        expect(bridge.calls.contains('connectPlayer'), isFalse);
        expect(remote.connectionInProgress, isFalse);
        expect(remote.authorized, isFalse);
        remote.dispose();
      },
    );
  }

  test(
    'choosing another player is explicit and does not start playback',
    () async {
      final bridge = ConnectionBridge();
      final remote = Remote(bridge)..accept(setupSnapshot('connected'));
      await remote.connectPlayer(apple: false);
      expect(bridge.calls, isEmpty);
      await remote.connectPlayer(apple: false, choose: true);
      expect(bridge.calls, ['permissions', 'searchPlayers']);
      remote.dispose();
    },
  );

  for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
    testWidgets('$platform Setup shows only the relevant connection step', (
      tester,
    ) async {
      debugDefaultTargetPlatformOverride = platform;
      addTearDown(() => debugDefaultTargetPlatformOverride = null);
      tester.view.physicalSize = platform == TargetPlatform.iOS
          ? const Size(420, 912)
          : const Size(320, 720);
      tester.view.devicePixelRatio = 1;
      tester.platformDispatcher.textScaleFactorTestValue =
          platform == TargetPlatform.iOS ? 3.2 : 2;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
      final remote = Remote(ConnectionBridge());
      await tester.pumpWidget(DX340App(remote: remote));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Setup').last);
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(find.text('Connect'), 150);
      expect(find.text('Connect'), findsOneWidget);
      expect(find.text('Find DX340'), findsNothing);
      expect(find.text('Retry authorization after approval'), findsNothing);
      remote.accept(setupSnapshot('approval'));
      await tester.pump();
      await tester.scrollUntilVisible(find.text('Cancel connection'), 150);
      expect(find.text('Connect to DX340'), findsNothing);
      expect(find.text('Cancel connection'), findsOneWidget);
      expect(find.text('Disconnect'), findsNothing);
      remote.accept(setupSnapshot('connected'));
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(find.text('Disconnect'), 150);
      expect(find.text('Open Player'), findsOneWidget);
      expect(find.text('Disconnect'), findsOneWidget);
      expect(find.text('Cancel connection'), findsNothing);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
      debugDefaultTargetPlatformOverride = null;
    });
  }
}
