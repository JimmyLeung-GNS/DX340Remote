import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/playback_progress.dart';
import 'package:dx340_phone/remote.dart';
import 'remote_test.dart' show FakeBridge, snapshot;

Json seekSample({
  String id = 'sample',
  int position = 25000,
  int duration = 60000,
  int playbackState = 2,
  bool playing = false,
  num speed = 0,
  bool advertised = true,
  int? age = 0,
}) {
  final value = <String, dynamic>{
    ...snapshot(
      source: 'com.apple.android.music',
      caps: {'canSeek': advertised},
    ),
  };
  value.addAll({'stateSampleId': id, 'stateAgeMs': age});
  value['state'] = {
    ...object(value['state']),
    'position': position,
    'duration': duration,
    'playbackState': playbackState,
    'playing': playing,
    'playbackSpeed': speed,
  };
  return value;
}

void main() {
  test(
    'seek rejects missing, unsupported or unprojectable source samples',
    () async {
      for (final sample in [
        seekSample(id: ''),
        seekSample(position: -1),
        seekSample(duration: 0),
        seekSample(playbackState: 0),
        seekSample(playbackState: 7),
        seekSample(advertised: false),
        seekSample(playbackState: 3, playing: false),
        seekSample(playbackState: 2, playing: true),
        seekSample(playbackState: 3, playing: true, speed: 0),
        seekSample(playbackState: 3, playing: true, speed: 1, age: null),
      ]) {
        final bridge = FakeBridge();
        final remote = Remote(bridge)..accept(sample);
        expect(remote.canSeekBackward, false);
        await remote.seekBackward();
        expect(bridge.calls, isEmpty);
        remote.dispose();
      }
    },
  );

  test(
    'paused lower boundary is clamped and a used sample cannot issue another seek',
    () async {
      final bridge = FakeBridge();
      final remote = Remote(bridge)..accept(seekSample(position: 5000));
      expect(remote.canSeekBackward, true);
      await remote.seekBackward();
      expect(bridge.calls.single.$2, {
        'type': 'SEEK',
        'args': {'position': 0},
      });
      expect(remote.canSeekBackward, false);
      await remote.seekBackward();
      remote.accept(seekSample(position: 5000));
      await remote.seekBackward();
      expect(bridge.calls.length, 1);
      remote.accept(seekSample(id: 'new', position: 20000));
      expect(remote.canSeekBackward, true);
      remote.accept(
        seekSample(id: 'new', position: 20000)..['authorized'] = false,
      );
      await remote.seekBackward();
      expect(bridge.calls.length, 1);
      remote.dispose();
    },
  );

  test(
    'playing seek uses the fresh local estimate and never sends playback',
    () async {
      var clock = 0;
      final bridge = FakeBridge();
      final remote = Remote(
        bridge,
        progress: PlaybackProgress(nowMs: () => clock),
      );
      remote.accept(
        seekSample(
          position: 10000,
          playbackState: 3,
          playing: true,
          speed: 1,
          age: 2000,
        ),
      );
      clock = 5000;
      await remote.seekBackward();
      expect(bridge.calls.single.$2, {
        'type': 'SEEK',
        'args': {'position': 7000},
      });
      expect(remote.progress.estimated, false);
      expect(remote.canSeekBackward, false);
      remote.setForeground(false);
      remote.accept(seekSample(id: 'background'));
      remote.setForeground(true);
      remote.accept(seekSample(id: 'background'));
      await remote.seekBackward();
      expect(bridge.calls.length, 1);
      remote.accept(seekSample(id: 'resumed'));
      expect(remote.canSeekBackward, true);
      remote.dispose();
    },
  );
}
