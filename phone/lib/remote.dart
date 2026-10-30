import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'playback_progress.dart';

typedef Json = Map<String, dynamic>;
Json object(dynamic value) =>
    value is Map ? Map<String, dynamic>.from(value) : {};
List<Json> objects(dynamic value) =>
    value is List ? value.map(object).toList() : [];
String textValue(dynamic value) => value is String ? value : '';

abstract interface class RemoteBridge {
  Stream<dynamic> get events;
  Future<dynamic> call(String method, [Json args = const {}]);
}

class NativeBridge implements RemoteBridge {
  static const _commands = MethodChannel('org.dx340.remote/commands');
  static const _events = EventChannel('org.dx340.remote/events');
  @override
  Stream<dynamic> get events => _events.receiveBroadcastStream();
  @override
  Future<dynamic> call(String method, [Json args = const {}]) =>
      _commands.invokeMethod(method, args);
}

/// Presentation state only. Native code owns BLE, request IDs, epochs and recovery.
class Remote extends ChangeNotifier {
  Remote(this.bridge, {PlaybackProgress? progress})
    : progress = progress ?? PlaybackProgress();
  final RemoteBridge bridge;
  final PlaybackProgress progress;
  StreamSubscription<dynamic>? _subscription;
  String status = 'Preparing Bluetooth remote…';
  String? error;
  bool authorized = false;
  bool mutationPending = false;
  bool controlRequestAvailable = false;
  bool _dispatchingControl = false;
  Json connectionSetup = {};
  bool _dispatchingConnection = false;
  int _connectionGeneration = 0;
  String get connectionPhase => authorized
      ? 'connected'
      : controlRequestAvailable
      ? 'busy'
      : _dispatchingConnection
      ? 'preparing'
      : textValue(connectionSetup['phase']).isEmpty
      ? 'idle'
      : textValue(connectionSetup['phase']);
  bool get connectionInProgress => {
    'preparing',
    'searching',
    'connecting',
    'approval',
  }.contains(connectionPhase);
  bool get hasSavedPlayer => connectionSetup['hasSavedPlayer'] == true;
  String get savedPlayerName => textValue(connectionSetup['savedName']).isEmpty
      ? 'player'
      : textValue(connectionSetup['savedName']);
  bool get canRequestControl =>
      controlRequestAvailable && !authorized && !busy && !_dispatchingControl;
  bool _dispatchingMutation = false;
  bool _disposed = false;
  Json state = {}, library = {};
  List<Json> sources = [], queue = [], devices = [];
  String queueRevision = '';
  Json queuePage = {};
  bool _nativeQueueLoading = false;
  bool _dispatchingQueue = false;
  bool get queueLoading => _nativeQueueLoading || _dispatchingQueue;
  int get queueOffset => queuePage['offset'] is int ? queuePage['offset'] : 0;
  int? get queueNextOffset =>
      queuePage['nextOffset'] is int ? queuePage['nextOffset'] : null;
  int? get queueTotal => queuePage['total'] is int ? queuePage['total'] : null;
  Uint8List? artwork;
  final List<String> _parents = [];
  String get scope =>
      '${state['source']}:${state['sourceEpoch']}:${caps['browseMode']}';
  Json get caps => object(state['capabilities']);
  bool get busy => mutationPending || _dispatchingMutation;
  bool can(String capability) => authorized && caps[capability] == true;
  bool action(String capability) => can(capability) && !busy;
  Json get volume => object(state['volume']);
  bool volumeStepAvailable(int delta) {
    if (!action('canAdjustVolume') ||
        volume['verified'] != true ||
        !{-1, 1}.contains(delta) ||
        textValue(volume['profileId']).isEmpty) {
      return false;
    }
    final current = volume['current'],
        low = volume['verifiedMinimum'],
        high = volume['verifiedMaximum'];
    return current is int &&
        low is int &&
        high is int &&
        low <= current &&
        current <= high &&
        current + delta >= low &&
        current + delta <= high;
  }

  Future<void> adjustVolume(int delta) async {
    if (!volumeStepAvailable(delta)) return;
    await command('ADJUST_VOLUME', {
      'delta': delta,
      'expectedVolume': volume['current'],
      'profileId': volume['profileId'],
    }, 'canAdjustVolume');
  }

  bool get assisted =>
      library['uiAssisted'] == true || caps['browseMode'] == 'ui-assisted';
  bool get assistedReady => can(
    caps['browseMode'] == 'ui-assisted' ? 'canBrowse' : 'canBrowseFolders',
  );
  String get browseExplanation =>
      textValue(caps['uiBrowseReason'] ?? caps['reason']);
  bool get libraryLoading => library['loading'] == true;
  bool get libraryInvalidated => library['invalidated'] == true;
  String get libraryRecovery => libraryInvalidated
      ? 'Page expired after selection. Tap Browse source library.'
      : '';
  String? get currentParent => textValue(library['requestedParent']).isEmpty
      ? null
      : textValue(library['requestedParent']);
  bool get canGoBack =>
      assisted ? textValue(library['backId']).isNotEmpty : _parents.isNotEmpty;
  int get page => library['page'] is num ? (library['page'] as num).toInt() : 0;
  int? get nextPage =>
      library['nextPage'] is num ? (library['nextPage'] as num).toInt() : null;

  Future<void> start() async {
    _subscription = bridge.events.listen(
      accept,
      onError: (Object issue) => _bridgeFailed(issue),
    );
    try {
      accept(await bridge.call('snapshot'));
    } catch (issue) {
      _bridgeFailed(issue);
    }
  }

  void accept(dynamic raw) {
    if (_disposed) return;
    try {
      final data = object(raw is String ? jsonDecode(raw) : raw);
      if (data['bridgeVersion'] != 1 ||
          data['authorized'] is! bool ||
          data['status'] is! String) {
        throw const FormatException('Incompatible native bridge');
      }
      final previousScope = scope;
      final wasAuthorized = authorized;
      state = object(data['state']);
      authorized = data['authorized'] == true;
      controlRequestAvailable = data['controlRequestAvailable'] == true;
      connectionSetup = object(data['connectionSetup']);
      mutationPending = data['mutationPending'] == true;
      status = data['status'];
      sources = objects(data['sources']);
      devices = objects(data['devices']);
      library = object(data['library']);
      queue = objects(data['queue']);
      queueRevision = textValue(data['queueRevision']);
      queuePage = object(data['queuePage']);
      _nativeQueueLoading = data['queueLoading'] == true;
      if (previousScope != scope || (wasAuthorized && !authorized)) {
        _parents.clear();
      }
      final encoded = textValue(data['artworkBase64']);
      artwork = encoded.isEmpty || encoded.length > 65536
          ? null
          : base64Decode(encoded);
      error = null;
      progress.accept(
        state: state,
        sampleId: textValue(data['stateSampleId']),
        ageMs: data['stateAgeMs'],
        authorized: authorized,
      );
      notifyListeners();
    } catch (issue) {
      _bridgeFailed(issue);
    }
  }

  void _bridgeFailed(Object issue) {
    if (_disposed) return;
    authorized = false;
    controlRequestAvailable = false;
    _connectionGeneration++;
    _dispatchingConnection = false;
    connectionSetup = {...connectionSetup, 'phase': 'failed'};
    progress.invalidate();
    mutationPending = false;
    _nativeQueueLoading = false;
    _parents.clear();
    error = issue is PlatformException
        ? issue.message
        : 'Connection data could not be read. Reopen the remote.';
    notifyListeners();
  }

  Future<bool> invoke(String method, [Json args = const {}]) async {
    try {
      await bridge.call(method, args);
      if (!_disposed) {
        error = null;
        notifyListeners();
      }
      return true;
    } catch (issue) {
      if (!_disposed) {
        error = issue is PlatformException
            ? issue.message
            : 'Action interrupted. Refresh before retrying.';
        notifyListeners();
      }
      return false;
    }
  }

  Future<void> requestControl() async {
    if (!canRequestControl) return;
    // Consume presentation availability before awaiting the platform channel.
    // Native code separately consumes the host-bound proposal exactly once.
    controlRequestAvailable = false;
    _dispatchingControl = true;
    notifyListeners();
    try {
      await invoke('requestControl');
    } finally {
      _dispatchingControl = false;
      if (!_disposed) notifyListeners();
    }
  }

  void setForeground(bool value) {
    progress.setForeground(value);
    if (!_disposed) notifyListeners();
  }

  Future<void> requestPermissions({required bool apple}) async {
    try {
      final granted = await bridge.call('permissions');
      if (!_disposed) {
        error = granted == true
            ? null
            : apple
            ? 'Allow Bluetooth access for DX340 Remote in iPhone Settings, then return to Setup.'
            : 'Allow Nearby Devices access in Android Settings, then return to Setup.';
        notifyListeners();
      }
    } catch (issue) {
      if (!_disposed) {
        error =
            'Permission request interrupted. Return to Setup and try again.';
        notifyListeners();
      }
    }
  }

  Future<void> connectPlayer({required bool apple, bool choose = false}) async {
    if (_disposed ||
        _dispatchingConnection ||
        connectionInProgress ||
        (authorized && !choose)) {
      return;
    }
    final ticket = ++_connectionGeneration;
    _dispatchingConnection = true;
    error = null;
    notifyListeners();
    try {
      final granted = await bridge.call('permissions');
      if (_disposed || ticket != _connectionGeneration) return;
      if (granted != true) {
        connectionSetup = {...connectionSetup, 'phase': 'permission'};
        error = apple
            ? 'Allow Bluetooth access in Settings, then tap Connect.'
            : 'Allow Nearby Devices access in Settings, then tap Connect.';
        return;
      }
      await bridge.call(choose ? 'searchPlayers' : 'connectPlayer');
    } catch (issue) {
      if (!_disposed && ticket == _connectionGeneration) {
        connectionSetup = {...connectionSetup, 'phase': 'failed'};
        error = issue is PlatformException
            ? issue.message
            : 'Connection interrupted. Tap Connect to try again.';
      }
    } finally {
      if (!_disposed && ticket == _connectionGeneration) {
        _dispatchingConnection = false;
        notifyListeners();
      }
    }
  }

  Future<void> cancelConnection() async {
    _connectionGeneration++;
    _dispatchingConnection = false;
    connectionSetup = {...connectionSetup, 'phase': 'idle'};
    await invoke('disconnect');
  }

  Future<bool> command(
    String type, [
    Json args = const {},
    String? capability,
  ]) async {
    if (!authorized || (capability != null && !can(capability))) return false;
    final mutation = !{'GET_STATE', 'GET_QUEUE'}.contains(type);
    if (mutation && busy) return false;
    if (mutation) {
      _dispatchingMutation = true;
      notifyListeners();
    }
    try {
      return await invoke('send', {'type': type, 'args': args});
    } finally {
      if (mutation) {
        _dispatchingMutation = false;
        if (!_disposed) notifyListeners();
      }
    }
  }

  Future<void> root() async {
    if (!can('canBrowse') || libraryLoading || busy) return;
    _parents.clear();
    await invoke('browse');
  }

  Future<void> open(Json item) async {
    if (!can('canBrowse') ||
        libraryLoading ||
        libraryInvalidated ||
        busy ||
        item['browsable'] != true) {
      return;
    }
    final id = textValue(item['id']);
    if (id.isEmpty) return;
    if ((assisted || item['uiAssisted'] == true) && !assistedReady) return;
    if (!assisted && item['uiAssisted'] != true) {
      _parents.add(currentParent ?? '');
    }
    await invoke('browse', {'parent': id});
  }

  Future<void> back() async {
    if (!canGoBack ||
        !can('canBrowse') ||
        libraryLoading ||
        libraryInvalidated ||
        busy ||
        (assisted && !assistedReady)) {
      return;
    }
    final parent = assisted
        ? textValue(library['backId'])
        : _parents.removeLast();
    await invoke('browse', {'parent': parent.isEmpty ? null : parent});
  }

  Future<void> more(int page) async {
    if (!can('canBrowse') ||
        libraryLoading ||
        libraryInvalidated ||
        busy ||
        page < 0 ||
        (assisted && !assistedReady)) {
      return;
    }
    await invoke('browse', {
      'parent': assisted ? library['parent'] : currentParent,
      'page': page,
    });
  }

  Future<void> playItem(
    Json item, {
    required String revision,
    required String sourceScope,
  }) async {
    if (libraryLoading ||
        libraryInvalidated ||
        revision.isEmpty ||
        revision != textValue(library['revision']) ||
        sourceScope != scope ||
        !objects(library['items']).any(
          (current) =>
              current['id'] == item['id'] &&
              current['title'] == item['title'] &&
              current['playable'] == true,
        ) ||
        item['playable'] != true ||
        ((assisted || item['uiAssisted'] == true) && !assistedReady)) {
      return;
    }
    await command('PLAY_ITEM', {
      'item': item['id'],
      'revision': revision,
    }, 'canPlayItem');
  }

  Future<void> playQueue(Json item, {required String revision}) async {
    if (queueLoading ||
        queueRevision.isEmpty ||
        revision != queueRevision ||
        !queue.any(
          (current) =>
              current['id'] == item['id'] && current['title'] == item['title'],
        )) {
      return;
    }
    await command('PLAY_QUEUE_ITEM', {
      'item': item['id'],
      'revision': revision,
    }, 'canPlayQueueItem');
  }

  Future<bool> readQueue([int offset = 0]) async {
    if (!can('canReadQueue') ||
        busy ||
        queueLoading ||
        offset < 0 ||
        (offset != 0 && (offset != queueNextOffset || queueRevision.isEmpty))) {
      return false;
    }
    _dispatchingQueue = true;
    notifyListeners();
    try {
      return await command('GET_QUEUE', {
        'offset': offset,
        if (offset != 0) 'revision': queueRevision,
      }, 'canReadQueue');
    } finally {
      _dispatchingQueue = false;
      if (!_disposed) notifyListeners();
    }
  }

  @override
  void dispose() {
    _disposed = true;
    _subscription?.cancel();
    super.dispose();
  }
}
