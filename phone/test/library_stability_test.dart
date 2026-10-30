import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show FakeBridge, snapshot;

Json librarySnapshot({
  String status = 'Library loaded.',
  bool pending = false,
}) => snapshot(
  mutation: pending,
  library: {
    'revision': 'album-a',
    'title': 'Album A',
    'items': [
      for (var i = 1; i <= 8; i++)
        {'id': 'song-$i', 'title': 'Song $i', 'playable': true},
    ],
  },
)..['status'] = status;

void main() {
  test(
    'a retained library row cannot borrow a new revision, page or source',
    () async {
      final bridge = FakeBridge();
      final remote = Remote(bridge)..accept(librarySnapshot());
      final item = objects(remote.library['items']).first;
      final scope = remote.scope;
      final newer = librarySnapshot();
      (newer['library'] as Json)['revision'] = 'album-b';
      remote.accept(newer);
      await remote.playItem(item, revision: 'album-a', sourceScope: scope);
      expect(bridge.calls, isEmpty);
      (newer['library'] as Json)['items'] = [
        {'id': 'other-song', 'title': 'Other song', 'playable': true},
      ];
      remote.accept(newer);
      await remote.playItem(item, revision: 'album-b', sourceScope: scope);
      expect(bridge.calls, isEmpty);
      final epochChanged = librarySnapshot();
      (epochChanged['state'] as Json)['sourceEpoch'] = 'new-epoch';
      remote.accept(epochChanged);
      await remote.playItem(item, revision: 'album-a', sourceScope: scope);
      expect(bridge.calls, isEmpty);
      await remote.playItem(
        item,
        revision: 'album-a',
        sourceScope: remote.scope,
      );
      expect(bridge.calls.single.$2, {
        'type': 'PLAY_ITEM',
        'args': {'item': 'song-1', 'revision': 'album-a'},
      });
      remote.dispose();
    },
  );
  for (final platform in [TargetPlatform.android, TargetPlatform.iOS]) {
    testWidgets(
      '$platform status and pending changes do not move library rows',
      (tester) async {
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
        remote.accept(librarySnapshot());
        await tester.tap(find.text('Library').last);
        await tester.pumpAndSettle();
        final initial = tester.getTopLeft(find.text('Song 1'));
        remote.accept(
          librarySnapshot(
            status:
                'Waiting for the source to confirm the selected song. '
                'Do not select another song while the outcome is unknown.',
            pending: true,
          ),
        );
        await tester.pump();
        expect(tester.getTopLeft(find.text('Song 1')), initial);
        await remote.requestPermissions(apple: platform == TargetPlatform.iOS);
        await tester.pump();
        expect(tester.getTopLeft(find.text('Song 1')), initial);
        remote.accept(
          librarySnapshot(status: 'Playback state confirmed by source.'),
        );
        await tester.pump();
        expect(tester.getTopLeft(find.text('Song 1')), initial);
        expect(bridge.calls.where((call) => call.$1 == 'send'), isEmpty);
        // A status-only update during a held tap must retain that song's target.
        final finger = await tester.startGesture(
          tester.getCenter(find.text('Song 1')),
        );
        remote.accept(librarySnapshot(status: 'Library ready.'));
        await tester.pump();
        await finger.up();
        await tester.pumpAndSettle();
        expect(bridge.calls.where((call) => call.$1 == 'send').single.$2, {
          'type': 'PLAY_ITEM',
          'args': {'item': 'song-1', 'revision': 'album-a'},
        });
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
        debugDefaultTargetPlatformOverride = null;
      },
    );
    testWidgets('$platform a page replacement cancels a held song tap', (
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
      remote.accept(librarySnapshot());
      await tester.tap(find.text('Library').last);
      await tester.pumpAndSettle();
      final finger = await tester.startGesture(
        tester.getCenter(find.text('Song 1')),
      );
      final next = librarySnapshot();
      (next['library'] as Json)['revision'] = 'replacement';
      (next['library'] as Json)['items'] = [
        {
          'id': 'replacement-song',
          'title': 'Replacement song',
          'playable': true,
        },
      ];
      remote.accept(next);
      await tester.pump();
      await finger.up();
      await tester.pumpAndSettle();
      expect(bridge.calls.where((call) => call.$1 == 'send'), isEmpty);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
      debugDefaultTargetPlatformOverride = null;
    });
  }
}
