import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show FakeBridge, snapshot;

void main() {
  for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
    Future<(Remote, FakeBridge)> mount(WidgetTester tester) async {
      debugDefaultTargetPlatformOverride = platform;
      addTearDown(() => debugDefaultTargetPlatformOverride = null);
      final bridge = FakeBridge();
      final remote = Remote(bridge);
      await tester.pumpWidget(DX340App(remote: remote));
      await tester.pumpAndSettle();
      return (remote, bridge);
    }

    Json state({
      bool playing = true,
      bool authorized = true,
      bool pause = true,
    }) {
      final data = snapshot(
        authorized: authorized,
        caps: {'canPlay': true, 'canPause': pause},
      );
      data['state'].addAll({
        'title':
            'A long synthetic track title for the persistent browsing controls',
        'artist': 'Synthetic artist',
        'playing': playing,
      });
      return data;
    }

    testWidgets('$platform keeps browsing controls reachable at large text', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(320, 720);
      tester.view.devicePixelRatio = 1;
      tester.platformDispatcher.textScaleFactorTestValue = 2.5;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
      final (remote, _) = await mount(tester);
      remote.accept(state());
      for (final destination in ['Library', 'Queue']) {
        await tester.tap(find.text(destination).last);
        await tester.pumpAndSettle();
        final bar = find.byKey(const ValueKey('browsing-playback-bar'));
        expect(bar, findsOneWidget);
        expect(
          find.descendant(of: bar, matching: find.byIcon(Icons.pause)),
          findsOneWidget,
        );
        expect(tester.takeException(), isNull);
        expect(tester.getBottomRight(bar).dy, lessThan(720));
      }
      await tester.tap(find.text('Setup').last);
      await tester.pumpAndSettle();
      expect(find.byKey(const ValueKey('browsing-playback-bar')), findsNothing);
      await tester.pumpWidget(const SizedBox());
      debugDefaultTargetPlatformOverride = null;
    });

    testWidgets(
      '$platform sends one desired pause and hides controls on disconnect',
      (tester) async {
        final (remote, bridge) = await mount(tester);
        remote.accept(state());
        await tester.tap(find.text('Library').last);
        await tester.pumpAndSettle();
        bridge.pending = Completer<dynamic>();
        final pause = find.byIcon(Icons.pause);
        await tester.tap(pause);
        await tester.tap(pause);
        await tester.pump();
        final sends = bridge.calls.where((call) => call.$1 == 'send').toList();
        expect(sends, hasLength(1));
        expect(sends.single.$2, {
          'type': 'SET_PLAYING',
          'args': {'playing': false},
        });
        remote.accept(state(authorized: false));
        await tester.pump();
        expect(
          find.byKey(const ValueKey('browsing-playback-bar')),
          findsNothing,
        );
        bridge.pending!.complete(null);
        await tester.pump();
        expect(bridge.calls.where((call) => call.$1 == 'send'), hasLength(1));
        await tester.pumpWidget(const SizedBox());
        debugDefaultTargetPlatformOverride = null;
      },
    );

    testWidgets('$platform respects pause capability and opens full Player', (
      tester,
    ) async {
      final (remote, _) = await mount(tester);
      remote.accept(state(pause: false));
      await tester.tap(find.text('Queue').last);
      await tester.pumpAndSettle();
      final icon = find.byIcon(Icons.pause);
      if (platform == TargetPlatform.android) {
        expect(
          tester
              .widget<IconButton>(
                find.ancestor(of: icon, matching: find.byType(IconButton)),
              )
              .onPressed,
          isNull,
        );
      } else {
        expect(
          tester
              .widget<CupertinoButton>(
                find.ancestor(of: icon, matching: find.byType(CupertinoButton)),
              )
              .onPressed,
          isNull,
        );
      }
      await tester.tap(
        find.text(
          'A long synthetic track title for the persistent browsing controls',
        ),
      );
      await tester.pumpAndSettle();
      expect(find.byKey(const ValueKey('browsing-playback-bar')), findsNothing);
      expect(
        find.text(
          'A long synthetic track title for the persistent browsing controls',
        ),
        findsOneWidget,
      );
      await tester.pumpWidget(const SizedBox());
      debugDefaultTargetPlatformOverride = null;
    });
  }
}
