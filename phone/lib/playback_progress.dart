/// Local presentation only; it never issues a playback or Bluetooth request.
class PlaybackProgress {
  PlaybackProgress({int Function()? nowMs}) : _nowMs = nowMs ?? _clock();
  static int Function() _clock() {
    final stopwatch = Stopwatch()..start();
    return () => stopwatch.elapsedMilliseconds;
  }

  static const _maxTime = 9007199254740991;
  final int Function() _nowMs;
  int? _base, durationMs;
  int _anchor = 0;
  double _rate = 0;
  String _sampleId = '';
  String? _blockedSampleId;
  (Object?, Object?)? _scope;
  bool _foreground = true;
  bool _authorized = false;

  static int? _milliseconds(dynamic value) =>
      value is num && value.isFinite && value >= 0 && value <= _maxTime
      ? value.toInt()
      : null;

  int? get positionMs {
    final base = _base;
    if (base == null) return null;
    final elapsed = (_nowMs() - _anchor).clamp(0, _maxTime);
    final position = base + elapsed * _rate;
    return position.clamp(0, durationMs ?? _maxTime).toInt();
  }

  bool get estimated => _rate > 0;
  bool get ticking => estimated && positionMs != durationMs;
  bool get needsRefresh =>
      _sampleId.isNotEmpty && _sampleId == _blockedSampleId;

  void _freeze() {
    _base = positionMs;
    _anchor = _nowMs();
    _rate = 0;
  }

  void invalidate() {
    _freeze();
    _authorized = false;
    _blockedSampleId = _sampleId;
  }

  void setForeground(bool value) {
    if (_foreground == value) return;
    _freeze();
    _foreground = value;
    // A fresh host state must arrive after returning to the foreground.
    _blockedSampleId = _sampleId;
  }

  void accept({
    required Map<String, dynamic> state,
    required String sampleId,
    required dynamic ageMs,
    required bool authorized,
  }) {
    final scope = (state['source'], state['sourceEpoch']);
    final changedScope = _scope != scope;
    if (!authorized && _authorized) invalidate();
    if (changedScope || sampleId.isEmpty || sampleId != _sampleId) {
      _scope = scope;
      _sampleId = sampleId;
      final duration = _milliseconds(state['duration']);
      durationMs = duration == 0 ? null : duration;
      _base = _milliseconds(state['position']);
      _anchor = _nowMs();
      _rate = 0;
      final age = _milliseconds(ageMs);
      final speed = state['playbackSpeed'];
      if (authorized &&
          _foreground &&
          sampleId.isNotEmpty &&
          sampleId != _blockedSampleId &&
          age != null &&
          _base != null &&
          durationMs != null &&
          state['playing'] == true &&
          speed is num &&
          speed.isFinite &&
          speed > 0 &&
          speed <= 16) {
        _rate = speed.toDouble();
        _anchor -= age;
      }
    }
    _authorized = authorized;
    if (!authorized) _blockedSampleId = sampleId;
  }
}
