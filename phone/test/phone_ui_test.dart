import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'package:dx340_phone/playback_progress.dart';
import 'remote_test.dart' show FakeBridge, snapshot;

void main() {
  for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
    testWidgets(
      '$platform can reach the last queue window and return to start',
      (tester) async {
        debugDefaultTargetPlatformOverride = platform;
        addTearDown(() => debugDefaultTargetPlatformOverride = null);
        final bridge = FakeBridge();
        final remote = Remote(bridge);
        await tester.pumpWidget(DX340App(remote: remote));
        await tester.pumpAndSettle();
        remote.accept(
          snapshot()..addAll({
            'queue': [
              {'id': '100', 'title': 'Last fixture track'},
            ],
            'queueRevision': 'q',
            'queuePage': {'offset': 100, 'total': 101, 'nextOffset': null},
          }),
        );
        await tester.tap(find.text('Queue').last);
        await tester.pumpAndSettle();
        expect(find.text('Queue items 101–101 of 101'), findsOneWidget);
        expect(find.text('Last fixture track'), findsOneWidget);
        expect(find.text('Next queue page'), findsNothing);
        await tester.tap(find.text('Start of queue'));
        await tester.pumpAndSettle();
        expect(bridge.calls.last.$2, {
          'type': 'GET_QUEUE',
          'args': {'offset': 0},
        });
        remote.accept(
          snapshot()..addAll({
            'queue': [
              {'id': '0', 'title': 'First fixture track'},
            ],
            'queueRevision': 'q2',
            'queuePage': {'offset': 0, 'total': 101, 'nextOffset': 1},
          }),
        );
        await tester.pumpAndSettle();
        await tester.tap(find.text('Next queue page'));
        await tester.pumpAndSettle();
        expect(bridge.calls.last.$2, {
          'type': 'GET_QUEUE',
          'args': {'offset': 1, 'revision': 'q2'},
        });
        await tester.pumpWidget(const SizedBox());
        debugDefaultTargetPlatformOverride = null;
      },
    );
    testWidgets(
      '$platform explains source permission recovery with disabled transport',
      (tester) async {
        debugDefaultTargetPlatformOverride = platform;
        addTearDown(() => debugDefaultTargetPlatformOverride = null);
        const reason =
            'On DX340, enable playback session access in Notification access settings, then refresh the player.';
        final remote = Remote(FakeBridge());
        await tester.pumpWidget(DX340App(remote: remote));
        await tester.pumpAndSettle();
        final data = snapshot(
          caps: {'canPlay': false, 'canBrowse': false, 'reason': reason},
        );
        data['state'] = {
          ...object(data['state']),
          'accessError': 'NOTIFICATION_ACCESS_REQUIRED',
        };
        remote.accept(data);
        await tester.pumpAndSettle();
        expect(find.text(reason), findsOneWidget);
        await tester.scrollUntilVisible(
          find.text('Play'),
          150,
          scrollable: find.byType(Scrollable).first,
        );
        if (platform == TargetPlatform.android) {
          expect(
            tester
                .widget<OutlinedButton>(
                  find.widgetWithText(OutlinedButton, 'Play'),
                )
                .onPressed,
            isNull,
          );
        } else {
          expect(
            tester
                .widget<CupertinoButton>(
                  find.widgetWithText(CupertinoButton, 'Play'),
                )
                .onPressed,
            isNull,
          );
        }
        await tester.tap(find.text('Library').last);
        await tester.pumpAndSettle();
        expect(find.text(reason), findsOneWidget);
        // Fresh capabilities after restoration reenable browsing; no command is replayed.
        remote.accept(snapshot(epoch: 'restored'));
        await tester.pumpAndSettle();
        expect(find.text(reason), findsNothing);
        expect(remote.can('canBrowse'), true);
        await tester.pumpWidget(const SizedBox());
        debugDefaultTargetPlatformOverride = null;
      },
    );
    testWidgets(
      '$platform elapsed clock freezes on suspend and adds no BLE polling',
      (tester) async {
        debugDefaultTargetPlatformOverride = platform;
        addTearDown(() => debugDefaultTargetPlatformOverride = null);
        var now = 0;
        final bridge = FakeBridge();
        final remote = Remote(
          bridge,
          progress: PlaybackProgress(nowMs: () => now),
        );
        await tester.pumpWidget(DX340App(remote: remote));
        await tester.pumpAndSettle();
        Json timing(String id, int position) {
          final data = snapshot();
          data['stateSampleId'] = id;
          data['stateAgeMs'] = 0;
          data['state'] = {
            ...object(data['state']),
            'position': position,
            'duration': 60000,
            'playing': true,
            'playbackSpeed': 1,
          };
          return data;
        }

        remote.accept(timing('first', 10000));
        await tester.pump();
        await tester.scrollUntilVisible(
          find.byType(ElapsedTime),
          150,
          scrollable: find.byType(Scrollable).first,
        );
        now += 2000;
        await tester.pump(const Duration(seconds: 2));
        expect(find.text('0:12 / 1:00 · estimated'), findsOneWidget);
        tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
        await tester.pump();
        now += 30000;
        await tester.pump(const Duration(seconds: 30));
        expect(remote.progress.positionMs, 12000);
        expect(remote.progress.estimated, false);
        tester.binding.handleAppLifecycleStateChanged(
          AppLifecycleState.resumed,
        );
        await tester.pump();
        expect(
          find.text('0:12 / 1:00 · refresh for current time'),
          findsOneWidget,
        );
        expect(bridge.calls.where((call) => call.$1 == 'resume').length, 1);
        expect(remote.progress.estimated, false);
        remote.accept(timing('fresh', 45000));
        await tester.pump();
        now += 1000;
        await tester.pump(const Duration(seconds: 1));
        expect(find.text('0:46 / 1:00 · estimated'), findsOneWidget);
        expect(bridge.calls.where((call) => call.$1 == 'send'), isEmpty);
        await tester.pumpWidget(const SizedBox());
        await tester.pump(const Duration(seconds: 5));
        expect(tester.takeException(), isNull);
        debugDefaultTargetPlatformOverride = null;
      },
    );
    testWidgets(
      '$platform disconnected library has working Setup recovery at large text',
      (tester) async {
        debugDefaultTargetPlatformOverride = platform;
        addTearDown(() => debugDefaultTargetPlatformOverride = null);
        await tester.pumpWidget(DX340App(remote: Remote(FakeBridge())));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Library').last);
        await tester.pumpAndSettle();
        expect(find.text('Open Setup'), findsOneWidget);
        await tester.tap(find.text('Open Setup'));
        await tester.pumpAndSettle();
        expect(find.text('Find DX340'), findsOneWidget);
        tester.platformDispatcher.textScaleFactorTestValue = 2.5;
        addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
        await tester.tap(find.text('Player').last);
        await tester.pumpAndSettle();
        await tester.scrollUntilVisible(
          find.text('Play'),
          200,
          scrollable: find.byType(Scrollable).first,
        );
        expect(tester.takeException(), isNull);
        if (platform == TargetPlatform.iOS) {
          expect(
            MediaQuery.textScalerOf(
              tester.element(find.text('Library').last),
            ).scale(10),
            10,
            reason:
                'Fixed-height iOS tabs follow Cupertino native sizing at accessibility text sizes.',
          );
          expect(
            MediaQuery.textScalerOf(
              tester.element(find.text('No track connected')),
            ).scale(10),
            greaterThan(10),
            reason:
                'Reading content must still respect the user’s larger text setting.',
          );
        }
        if (platform == TargetPlatform.android) {
          expect(
            tester
                .widget<OutlinedButton>(
                  find.widgetWithText(OutlinedButton, 'Play'),
                )
                .onPressed,
            isNull,
          );
        } else {
          expect(
            tester
                .widget<CupertinoButton>(
                  find.widgetWithText(CupertinoButton, 'Play'),
                )
                .onPressed,
            isNull,
          );
        }
        await tester.pumpWidget(const SizedBox());
        debugDefaultTargetPlatformOverride = null;
      },
    );
  }
}
