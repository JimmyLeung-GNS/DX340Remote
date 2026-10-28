import 'package:flutter_test/flutter_test.dart';
import 'package:dx340_phone/playback_progress.dart';

void main() {
  late int now;
  late PlaybackProgress progress;
  var serial = 0;
  Map<String, dynamic> state({
    int position = 10000,
    int duration = 60000,
    num speed = 1,
    bool playing = true,
    String source = 'mango',
    String epoch = 'a',
  }) => {
    'source': source,
    'sourceEpoch': epoch,
    'position': position,
    'duration': duration,
    'playing': playing,
    'playbackSpeed': speed,
  };

  void accept(
    Map<String, dynamic> value, {
    String? id,
    int? age = 0,
    bool authorized = true,
  }) => progress.accept(
    state: value,
    sampleId: id ?? '${++serial}',
    ageMs: age,
    authorized: authorized,
  );

  setUp(() {
    now = 0;
    progress = PlaybackProgress(nowMs: () => now);
  });

  test('advances locally at source speed including native sample age', () {
    accept(state(speed: 2), age: 3000);
    expect(progress.positionMs, 16000);
    now += 2000;
    expect(progress.positionMs, 20000);
    expect(progress.estimated, true);
  });

  test('status/artwork events cannot restart or rewind the same sample', () {
    accept(state(), id: 'same');
    now += 5000;
    accept(state(), id: 'same', age: 5000);
    expect(progress.positionMs, 15000);
    now += 5000;
    expect(progress.positionMs, 20000);
  });

  test('pause, seek, track change and source epoch replace the estimate', () {
    accept(state());
    now += 5000;
    accept(state(position: 15000, playing: false));
    now += 2000;
    expect(progress.positionMs, 15000);
    expect(progress.estimated, false);
    accept(state(position: 3000));
    expect(progress.positionMs, 3000);
    accept(state(position: 0, duration: 120000));
    now += 1000;
    expect(progress.positionMs, 1000);
    accept(state(source: 'apple', epoch: 'b', position: 20000));
    expect(progress.positionMs, 20000);
  });

  test('connection loss freezes and reauthorization needs a fresh sample', () {
    accept(state(), id: 'old');
    now += 4000;
    accept(state(), id: 'old', authorized: false);
    now += 5000;
    expect(progress.positionMs, 14000);
    accept(state(), id: 'old');
    expect(progress.estimated, false);
    accept(state(position: 20000), id: 'fresh');
    now += 1000;
    expect(progress.positionMs, 21000);
  });

  test('foreground resume waits for a state received after resume', () {
    accept(state(), id: 'before');
    now += 3000;
    progress.setForeground(false);
    now += 60000;
    expect(progress.positionMs, 13000);
    accept(state(position: 30000), id: 'background');
    progress.setForeground(true);
    accept(state(position: 30000), id: 'background', age: 5000);
    expect(progress.estimated, false);
    accept(state(position: 40000), id: 'resumed');
    now += 1000;
    expect(progress.positionMs, 41000);
  });

  test('legacy hosts and cached metadata remain source snapshots', () {
    accept(state()..remove('playbackSpeed'));
    now += 5000;
    expect(progress.positionMs, 10000);
    accept(state(), id: '', age: null);
    now += 5000;
    expect(progress.positionMs, 10000);
    expect(progress.estimated, false);
  });

  test('unknown/invalid position, duration, clock or rate cannot advance', () {
    accept(state(position: -1));
    expect(progress.positionMs, isNull);
    accept(state(duration: 0));
    expect(progress.durationMs, isNull);
    expect(progress.estimated, false);
    accept(state(), age: -1);
    expect(progress.estimated, false);
    for (final rate in [double.nan, double.infinity, -1, 0, 17]) {
      accept(state(speed: rate));
      expect(progress.estimated, false);
    }
  });

  test(
    'track end stays bounded and never advances to an invented next track',
    () {
      accept(state(position: 59000));
      now += 60000;
      expect(progress.positionMs, 60000);
      expect(progress.ticking, false);
      accept(state(position: 70000, playing: false));
      expect(progress.positionMs, 60000);
      progress.invalidate();
      now += 10000;
      expect(progress.positionMs, 60000);
      expect(progress.estimated, false);
    },
  );
}
