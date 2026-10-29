import 'dart:async';
import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/remote.dart';

class FakeBridge implements RemoteBridge {
  final stream = StreamController<dynamic>.broadcast(sync: true);
  final calls = <(String, Json)>[];
  Completer<dynamic>? pending;
  @override
  Stream<dynamic> get events => stream.stream;
  @override
  Future<dynamic> call(String method, [Json args = const {}]) async {
    calls.add((method, args));
    if (method == 'snapshot') return snapshot(authorized: false);
    if (method == 'captureTab') return null;
    return pending?.future;
  }
}

Json snapshot({
  bool authorized = true,
  String source = 'mango',
  String epoch = 'a',
  Json? library,
  Json? caps,
  bool mutation = false,
}) => {
  'bridgeVersion': 1,
  'status': 'Test fixture — no physical connection',
  'authorized': authorized,
  'mutationPending': mutation,
  'state': {
    'source': source,
    'sourceEpoch': epoch,
    'capabilities':
        caps ??
        {
          'canPlay': true,
          'canBrowse': true,
          'canPlayItem': true,
          'canPlayQueueItem': true,
          'canReadQueue': true,
        },
  },
  'library': library ?? {},
};

void main() {
  test(
    'verified volume range and identity must accompany each deliberate step',
    () async {
      final bridge = FakeBridge();
      final remote = Remote(bridge);
      Json volumeSnapshot({bool authorized = true, bool mutation = false}) {
        final data = snapshot(
          authorized: authorized,
          mutation: mutation,
          caps: {'canAdjustVolume': true},
        );
        data['state']['volume'] = {
          'current': 35,
          'verified': true,
          'verifiedMinimum': 34,
          'verifiedMaximum': 35,
          'profileId': 'local-proof',
        };
        return data;
      }

      remote.accept(volumeSnapshot());
      await remote.adjustVolume(1);
      expect(bridge.calls, isEmpty);
      await remote.adjustVolume(-1);
      expect(bridge.calls.single.$2, {
        'type': 'ADJUST_VOLUME',
        'args': {'delta': -1, 'expectedVolume': 35, 'profileId': 'local-proof'},
      });
      bridge.calls.clear();
      remote.accept(volumeSnapshot(authorized: false));
      await remote.adjustVolume(-1);
      expect(bridge.calls, isEmpty);
      remote.accept(volumeSnapshot(mutation: true));
      await remote.adjustVolume(-1);
      expect(bridge.calls, isEmpty);
      final bad = volumeSnapshot();
      bad['state']['volume']['current'] = '35';
      remote.accept(bad);
      await remote.adjustVolume(-1);
      expect(bridge.calls, isEmpty);
      remote.dispose();
    },
  );
  test(
    'volume double tap and reconnection do not replay a pending step',
    () async {
      final bridge = FakeBridge()..pending = Completer();
      final remote = Remote(bridge);
      final data = snapshot(caps: {'canAdjustVolume': true});
      data['state']['volume'] = {
        'current': 35,
        'verified': true,
        'verifiedMinimum': 34,
        'verifiedMaximum': 35,
        'profileId': 'proof',
      };
      remote.accept(data);
      final first = remote.adjustVolume(-1);
      await remote.adjustVolume(-1);
      expect(bridge.calls.length, 1);
      remote.accept(snapshot(authorized: false));
      bridge.pending!.complete();
      await first;
      remote.accept(data);
      expect(bridge.calls.length, 1);
      remote.dispose();
    },
  );
  test(
    'cached assisted pages cannot borrow native Mango capabilities after UI loss',
    () async {
      final bridge = FakeBridge();
      final remote = Remote(bridge)
        ..accept(
          snapshot(
            caps: {
              'canBrowse': true,
              'canPlayItem': true,
              'canNext': true,
              'canBrowseFolders': false,
              'browseMode': 'direct',
            },
            library: {
              'uiAssisted': true,
              'backId': 'old-back',
              'parent': 'old-scroll',
              'revision': 'old-ui',
              'nextPage': 1,
            },
          ),
        );
      expect(remote.assistedReady, false);
      await remote.open({'id': 'old-folder', 'browsable': true});
      await remote.back();
      await remote.more(1);
      await remote.playItem({'id': 'old-file', 'playable': true});
      expect(bridge.calls, isEmpty);
      expect(await remote.command('NEXT', {}, 'canNext'), true);
      await remote.root();
      expect(bridge.calls.map((call) => call.$1), ['send', 'browse']);
      remote.dispose();
    },
  );
  test(
    'cached optional folder entries stay disabled while native albums remain usable',
    () async {
      final bridge = FakeBridge();
      final remote = Remote(bridge)
        ..accept(
          snapshot(
            caps: {
              'canBrowse': true,
              'canPlayItem': true,
              'canBrowseFolders': false,
              'browseMode': 'direct',
            },
          ),
        );
      await remote.open({
        'id': 'mango-ui:root',
        'browsable': true,
        'uiAssisted': true,
      });
      await remote.playItem({
        'id': 'ui-file',
        'playable': true,
        'uiAssisted': true,
      });
      expect(bridge.calls, isEmpty);
      await remote.open({'id': 'native-album', 'browsable': true});
      expect(bridge.calls.single.$2, {'parent': 'native-album'});
      remote.dispose();
    },
  );
  test(
    'a retained queue row cannot select using a newer revision or another page',
    () async {
      final bridge = FakeBridge();
      final item = {'id': 'reused', 'title': 'Original title'};
      final remote = Remote(bridge)
        ..accept(
          snapshot()..addAll({
            'queue': [item],
            'queueRevision': 'new',
          }),
        );
      await remote.playQueue(item, revision: 'old');
      await remote.playQueue({
        'id': 'off-page',
        'title': 'Other title',
      }, revision: 'new');
      await remote.playQueue({
        'id': 'reused',
        'title': 'Wrong title',
      }, revision: 'new');
      expect(bridge.calls, isEmpty);
      await remote.playQueue(item, revision: 'new');
      expect(bridge.calls.single.$2, {
        'type': 'PLAY_QUEUE_ITEM',
        'args': {'item': 'reused', 'revision': 'new'},
      });
      remote.dispose();
    },
  );
  test(
    'queue continuation carries revision and cannot use arbitrary offsets or stale scope',
    () async {
      final bridge = FakeBridge();
      final data = snapshot()
        ..addAll({
          'queue': [
            {'id': '0', 'title': 'Fixture'},
          ],
          'queueRevision': 'q1',
          'queuePage': {'offset': 0, 'nextOffset': 50, 'total': 101},
        });
      final remote = Remote(bridge)..accept(data);
      expect(remote.queueNextOffset, 50);
      expect(remote.queueTotal, 101);
      expect(await remote.readQueue(25), false);
      expect(await remote.readQueue(50), true);
      expect(bridge.calls.single.$2, {
        'type': 'GET_QUEUE',
        'args': {'offset': 50, 'revision': 'q1'},
      });
      remote.accept(snapshot(source: 'apple', epoch: 'new'));
      expect(remote.queueNextOffset, isNull);
      expect(await remote.readQueue(50), false);
      expect(bridge.calls.length, 1);
      remote.dispose();
    },
  );
  test(
    'queue loading prevents duplicate reads and old-row selection',
    () async {
      final bridge = FakeBridge()..pending = Completer();
      final remote = Remote(bridge)
        ..accept(snapshot()..addAll({'queueRevision': 'q'}));
      final read = remote.readQueue();
      expect(remote.queueLoading, true);
      expect(await remote.readQueue(), false);
      await remote.playQueue({'id': 'old'}, revision: 'q');
      expect(bridge.calls.length, 1);
      bridge.pending!.complete();
      await read;
      remote.accept(
        snapshot()..addAll({'queueRevision': 'q', 'queueLoading': true}),
      );
      expect(await remote.readQueue(), false);
      await remote.playQueue({'id': 'old'}, revision: 'q');
      expect(bridge.calls.length, 1);
      remote.accept(snapshot(authorized: false));
      expect(await remote.readQueue(), false);
      remote.dispose();
    },
  );
  test(
    'legacy queue replies have no invented continuation and refresh starts from zero',
    () async {
      final bridge = FakeBridge();
      final remote = Remote(bridge)
        ..accept(
          snapshot()..addAll({
            'queueRevision': 'legacy',
            'queue': [
              {'id': 'a'},
            ],
          }),
        );
      expect(remote.queueTotal, isNull);
      expect(remote.queueNextOffset, isNull);
      await remote.readQueue();
      expect(bridge.calls.single.$2, {
        'type': 'GET_QUEUE',
        'args': {'offset': 0},
      });
      remote.dispose();
    },
  );
  test(
    'hybrid Mango folders return to native root without retaining a stale native parent',
    () async {
      final bridge = FakeBridge();
      final remote = Remote(bridge)
        ..accept(
          snapshot(
            caps: {
              'canBrowse': true,
              'browseMode': 'direct',
              'canBrowseFolders': true,
              'uiBrowseReason': 'Mango needs DX340 awake',
            },
          ),
        );
      await remote.open({
        'id': 'mango-ui:root',
        'browsable': true,
        'uiAssisted': true,
      });
      expect(remote.canGoBack, false);
      remote.accept(
        snapshot(
          caps: {
            'canBrowse': true,
            'browseMode': 'direct',
            'canBrowseFolders': true,
            'uiBrowseReason': 'Mango needs DX340 awake',
          },
          library: {
            'uiAssisted': true,
            'backId': 'mango-native:root',
            'revision': 'ui-r',
          },
        ),
      );
      expect(remote.assisted, true);
      expect(remote.browseExplanation, 'Mango needs DX340 awake');
      await remote.back();
      expect(bridge.calls.last.$2, {'parent': 'mango-native:root'});
      remote.accept(
        snapshot(
          library: {'items': [], 'requestedParent': 'mango-native:root'},
        ),
      );
      expect(remote.assisted, false);
      expect(remote.canGoBack, false);
      remote.dispose();
    },
  );
  test('cached capabilities cannot authorize commands', () async {
    final bridge = FakeBridge();
    final remote = Remote(bridge);
    remote.accept(snapshot(authorized: false));
    expect(remote.can('canPlay'), false);
    expect(
      await remote.command('SET_PLAYING', {'playing': true}, 'canPlay'),
      false,
    );
    expect(bridge.calls, isEmpty);
    // Assisted selection can be available before native Mango play-by-ID is verified.
    // The diagnostic-only testable flag must never authorize phone playback.
    remote.accept(snapshot());
    await remote.playItem({
      'id': 'native-unverified',
      'playable': false,
      'nativeTestable': true,
    });
    expect(bridge.calls, isEmpty);
    await remote.playItem({'id': 'ui-verified', 'playable': true});
    expect(bridge.calls.single.$1, 'send');
    expect(bridge.calls.single.$2['args']['item'], 'ui-verified');
    remote.dispose();
  });
  test(
    'a double tap cannot dispatch two mutations while bridge call is pending',
    () async {
      final bridge = FakeBridge()..pending = Completer();
      final remote = Remote(bridge)..accept(snapshot());
      final first = remote.command('SET_PLAYING', {'playing': true}, 'canPlay');
      expect(await remote.command('NEXT'), false);
      expect(bridge.calls.length, 1);
      bridge.pending!.complete();
      await first;
      remote.dispose();
    },
  );
  test(
    'native pending mutation disables actions but a read-only artwork event does not',
    () {
      final remote = Remote(FakeBridge())..accept(snapshot(mutation: true));
      expect(remote.action('canPlay'), false);
      remote.accept(snapshot());
      expect(remote.action('canPlay'), true);
      remote.dispose();
    },
  );
  test(
    'connection loss discards navigation and never replays playback on resume',
    () async {
      final bridge = FakeBridge();
      final remote = Remote(bridge)..accept(snapshot());
      await remote.open({'id': 'album', 'browsable': true});
      expect(remote.canGoBack, true);
      remote.accept(snapshot(authorized: false));
      expect(remote.canGoBack, false);
      remote.accept(snapshot());
      await remote.invoke('resume');
      expect(bridge.calls.map((c) => c.$1), ['browse', 'resume']);
      remote.dispose();
    },
  );
  test('changing source or epoch clears parent navigation', () async {
    final remote = Remote(FakeBridge())..accept(snapshot());
    await remote.open({'id': 'folder', 'browsable': true});
    expect(remote.canGoBack, true);
    remote.accept(snapshot(epoch: 'new'));
    expect(remote.canGoBack, false);
    await remote.open({'id': 'folder', 'browsable': true});
    remote.accept(snapshot(source: 'apple'));
    expect(remote.canGoBack, false);
    remote.dispose();
  });
  test(
    'UI pages use back and scroll tokens and correct play-item wire key',
    () async {
      final bridge = FakeBridge();
      final remote = Remote(bridge)
        ..accept(
          snapshot(
            source: 'apple',
            caps: {
              'canBrowse': true,
              'canPlayItem': true,
              'browseMode': 'ui-assisted',
            },
            library: {
              'uiAssisted': true,
              'backId': 'back-token',
              'parent': 'scroll-token',
              'revision': 'r1',
              'nextPage': 2,
            },
          ),
        );
      await remote.back();
      await remote.more(2);
      await remote.playItem({'id': 'song-token', 'playable': true});
      expect(bridge.calls.map((c) => c.$1), ['browse', 'browse', 'send']);
      expect(bridge.calls[0].$2, {'parent': 'back-token'});
      expect(bridge.calls[1].$2, {'parent': 'scroll-token', 'page': 2});
      expect(bridge.calls[2].$2, {
        'type': 'PLAY_ITEM',
        'args': {'item': 'song-token', 'revision': 'r1'},
      });
      remote.dispose();
    },
  );
  test('malformed bridge data fails closed', () {
    final remote = Remote(FakeBridge())..accept(snapshot());
    remote.accept('{invalid');
    expect(remote.authorized, false);
    expect(remote.error, isNotNull);
    remote.accept({
      'bridgeVersion': 2,
      'authorized': true,
      'status': 'Wrong bridge',
    });
    expect(remote.authorized, false);
    remote.dispose();
  });
}
