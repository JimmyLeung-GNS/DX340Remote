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
      expect(bridge.calls[2].$2, {'type': 'PLAY_ITEM', 'args': {'item': 'song-token', 'revision': 'r1'}});
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
