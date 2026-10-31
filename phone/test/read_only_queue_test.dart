import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show FakeBridge, snapshot;

Json queueSnapshot({required bool browse, required bool selectable}) =>
    snapshot(
        caps: {
          'canReadQueue': true,
          'canPlayQueueItem': selectable,
          'canBrowse': browse,
        },
      )
      ..['status'] = 'Queue loaded.'
      ..['queueRevision'] = 'queue-a'
      ..['queuePage'] = {'offset': 0, 'total': 2, 'nextOffset': null}
      ..['queue'] = [
        {'id': 'q1', 'title': 'Song 1'},
        {'id': 'q2', 'title': 'Song 2'},
      ];

void main() {
  for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
    for (final browse in [true, false]) {
      testWidgets('$platform read-only queue guides selection browse=$browse', (
        tester,
      ) async {
        debugDefaultTargetPlatformOverride = platform;
        addTearDown(() => debugDefaultTargetPlatformOverride = null);
        tester.view.physicalSize = const Size(400, 1000);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        final bridge = FakeBridge();
        final remote = Remote(bridge);
        await tester.pumpWidget(DX340App(remote: remote));
        await tester.pumpAndSettle();
        remote.accept(queueSnapshot(browse: browse, selectable: false));
        await tester.tap(find.text('Queue').last);
        await tester.pumpAndSettle();
        final semantics = tester.ensureSemantics();
        try {
          final guidance = browse
              ? 'Read-only queue. Choose tracks in Library.'
              : 'Read-only queue. Choose tracks on your player.';
          expect(find.text(guidance), findsOneWidget);
          expect(find.bySemanticsLabel(RegExp('Queue track')), findsWidgets);
          expect(find.bySemanticsLabel(RegExp(r'Play\nSong 1')), findsNothing);
          final rowPosition = tester.getTopLeft(find.text('Song 1'));
          bridge.calls.clear();
          await tester.tap(find.text('Song 1'), warnIfMissed: false);
          await tester.pumpAndSettle();
          expect(bridge.calls, isEmpty);
          remote.accept(queueSnapshot(browse: browse, selectable: true));
          await tester.pumpAndSettle();
          expect(find.text(guidance), findsNothing);
          expect(tester.getTopLeft(find.text('Song 1')), rowPosition);
          await tester.tap(find.text('Song 1'));
          await tester.pumpAndSettle();
          expect(bridge.calls.single.$2, {
            'type': 'PLAY_QUEUE_ITEM',
            'args': {'item': 'q1', 'revision': 'queue-a'},
          });
          expect(tester.takeException(), isNull);
          await tester.pumpWidget(const SizedBox());
        } finally {
          semantics.dispose();
          debugDefaultTargetPlatformOverride = null;
        }
      });
    }
  }
}
