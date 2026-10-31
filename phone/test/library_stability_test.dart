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
  for (final source in ['com.ibasso.music', 'com.apple.android.music']) {
    test(
      '$source expired assisted page allows fresh browsing but no stale action',
      () async {
        final bridge = FakeBridge();
        final remote = Remote(bridge);
        Json currentPage() {
          final page = librarySnapshot();
          (page['state'] as Json)['source'] = source;
          (page['state'] as Json)['capabilities'] = {
            'canBrowse': true,
            'browseMode': source == 'com.apple.android.music'
                ? 'ui-assisted'
                : 'direct',
            'canBrowseFolders': source == 'com.ibasso.music',
            'canPlayItem': true,
            'canReadQueue': true,
            'canPause': true,
          };
          (page['library'] as Json)['uiAssisted'] = true;
          return page;
        }

        final expired = currentPage();
        (expired['library'] as Json).addAll({
          'invalidated': true,
          'uiAssisted': true,
          'backId': 'old-back',
          'nextPage': 1,
        });
        remote.accept(expired);
        await remote.back();
        await remote.more(1);
        await remote.open({'id': 'old-folder', 'browsable': true});
        await remote.playItem(
          objects(remote.library['items']).first,
          revision: 'album-a',
          sourceScope: remote.scope,
        );
        expect(bridge.calls, isEmpty);
        // Only the retired page is blocked; playback and fresh root browsing remain usable.
        await remote.command('SET_PLAYING', {'playing': false}, 'canPause');
        await remote.root();
        expect(bridge.calls.map((call) => call.$1), ['send', 'browse']);
        remote.accept(currentPage());
        expect(remote.libraryInvalidated, false);
        await remote.playItem(
          objects(remote.library['items']).first,
          revision: 'album-a',
          sourceScope: remote.scope,
        );
        expect(bridge.calls.last.$2['type'], 'PLAY_ITEM');
        remote.dispose();
      },
    );
  }
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
      '$platform interrupted browse stops loading and reconnect never replays it',
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
        remote.accept(snapshot(library: {'loading': true}));
        await tester.tap(find.text('Library').last);
        await tester.pump(const Duration(milliseconds: 100));
        expect(remote.libraryLoading, true);
        expect(find.byType(CircularProgressIndicator), findsOneWidget);
        final lost = snapshot(authorized: false, library: {'loading': true})
          ..['connectionSetup'] = {'phase': 'idle', 'hasSavedPlayer': true};
        remote.accept(lost);
        await tester.pumpAndSettle();
        expect(remote.libraryLoading, false);
        expect(find.byType(CircularProgressIndicator), findsNothing);
        expect(
          find.text('Reconnect in Setup to browse music on DX340.'),
          findsOneWidget,
        );
        expect(bridge.calls.where((call) => call.$1 == 'browse'), isEmpty);
        remote.accept(snapshot());
        await tester.pumpAndSettle();
        expect(find.byType(CircularProgressIndicator), findsNothing);
        expect(bridge.calls.where((call) => call.$1 == 'browse'), isEmpty);
        await tester.tap(find.text('Browse source library'));
        await tester.pumpAndSettle();
        expect(bridge.calls.where((call) => call.$1 == 'browse').length, 1);
        // Completed cached pages are retained on loss, with actions disabled.
        final completed = snapshot(
          authorized: false,
          library: {
            'title': 'Cached album',
            'revision': 'old',
            'items': [
              {'id': 'old-song', 'title': 'Cached song', 'playable': true},
            ],
          },
        );
        remote.accept(completed);
        await tester.pumpAndSettle();
        expect(find.text('Cached song'), findsOneWidget);
        await tester.tap(find.text('Cached song'));
        await tester.pumpAndSettle();
        expect(bridge.calls.where((call) => call.$1 == 'send'), isEmpty);
        expect(bridge.calls.where((call) => call.$1 == 'browse').length, 1);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
        debugDefaultTargetPlatformOverride = null;
      },
    );
    testWidgets(
      '$platform retiring a page keeps rows fixed and cancels a held tap',
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
        final current = librarySnapshot();
        (current['state'] as Json)['capabilities'] = {
          'canBrowse': true,
          'canBrowseFolders': true,
          'canPlayItem': true,
        };
        (current['library'] as Json).addAll({
          'uiAssisted': true,
          'backId': 'old-back',
        });
        remote.accept(current);
        await tester.tap(find.text('Library').last);
        await tester.pumpAndSettle();
        bridge.calls.clear();
        final initial = tester.getTopLeft(find.text('Song 1'));
        final finger = await tester.startGesture(
          tester.getCenter(find.text('Song 1')),
        );
        (current['library'] as Json)['invalidated'] = true;
        remote.accept(current);
        await tester.pump();
        expect(tester.getTopLeft(find.text('Song 1')), initial);
        expect(find.text(remote.libraryRecovery), findsOneWidget);
        await finger.up();
        await tester.pumpAndSettle();
        await tester.tap(find.text('Back to parent'));
        await tester.pumpAndSettle();
        expect(bridge.calls, isEmpty);
        await tester.tap(find.text('Browse source library'));
        await tester.pumpAndSettle();
        expect(bridge.calls.single.$1, 'browse');
        expect(bridge.calls.single.$2, isEmpty);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
        debugDefaultTargetPlatformOverride = null;
      },
    );
    testWidgets(
      '$platform a previous folder offset cannot return in a new album',
      (tester) async {
        debugDefaultTargetPlatformOverride = platform;
        addTearDown(() => debugDefaultTargetPlatformOverride = null);
        tester.view.physicalSize = const Size(400, 1000);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        final remote = Remote(FakeBridge());
        await tester.pumpWidget(DX340App(remote: remote));
        await tester.pumpAndSettle();
        final folders = librarySnapshot();
        (folders['library'] as Json)['requestedParent'] = 'previous-folder';
        (folders['library'] as Json)['items'] = [
          for (var i = 0; i < 30; i++)
            {
              'id': 'folder-$i',
              'title': 'Previous folder row $i',
              'browsable': true,
            },
        ];
        remote.accept(folders);
        await tester.tap(find.text('Library').last);
        await tester.pumpAndSettle();
        await tester.drag(find.byType(ListView), const Offset(0, -350));
        await tester.pumpAndSettle();
        expect(
          tester
              .state<ScrollableState>(
                find.descendant(
                  of: find.byType(ListView),
                  matching: find.byType(Scrollable),
                ),
              )
              .position
              .pixels,
          greaterThan(100),
        );
        remote.accept(snapshot(library: {'loading': true}));
        // A cached reply can arrive before Flutter renders the loading frame.
        final album = librarySnapshot();
        (album['library'] as Json)['requestedParent'] = 'new-album';
        (album['library'] as Json)['items'] = [
          for (var i = 1; i <= 30; i++)
            {'id': 'song-$i', 'title': 'Song $i', 'playable': true},
        ];
        remote.accept(album);
        await tester.pumpAndSettle();
        expect(
          tester
              .state<ScrollableState>(
                find.descendant(
                  of: find.byType(ListView),
                  matching: find.byType(Scrollable),
                ),
              )
              .position
              .pixels,
          0,
        );
        final beforeLeaving = tester.getTopLeft(find.text('Song 1'));
        await tester.tap(find.text('Player').last);
        await tester.pumpAndSettle();
        await tester.tap(find.text('Library').last);
        await tester.pumpAndSettle();
        expect(tester.getTopLeft(find.text('Song 1')), beforeLeaving);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox());
        debugDefaultTargetPlatformOverride = null;
      },
    );
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
