import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

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
  Remote(this.bridge);
  final RemoteBridge bridge;
  StreamSubscription<dynamic>? _subscription;
  String status = 'Preparing Bluetooth remote…';
  String? error;
  bool authorized = false;
  bool mutationPending = false;
  bool _dispatchingMutation = false;
  bool _disposed = false;
  Json state = {}, library = {};
  List<Json> sources = [], queue = [], devices = [];
  String queueRevision = '';
  Uint8List? artwork;
  final List<String> _parents = [];
  String get scope =>
      '${state['source']}:${state['sourceEpoch']}:${caps['browseMode']}';
  Json get caps => object(state['capabilities']);
  bool get busy => mutationPending || _dispatchingMutation;
  bool can(String capability) => authorized && caps[capability] == true;
  bool action(String capability) => can(capability) && !busy;
  bool get assisted =>
      library['uiAssisted'] == true || caps['browseMode'] == 'ui-assisted';
  bool get libraryLoading => library['loading'] == true;
  String? get currentParent => textValue(library['requestedParent']).isEmpty
      ? null
      : textValue(library['requestedParent']);
  bool get canGoBack =>
      assisted ? textValue(library['backId']).isNotEmpty : _parents.isNotEmpty;
  int get page => (library['page'] as num?)?.toInt() ?? 0;
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
      mutationPending = data['mutationPending'] == true;
      status = data['status'];
      sources = objects(data['sources']);
      devices = objects(data['devices']);
      library = object(data['library']);
      queue = objects(data['queue']);
      queueRevision = textValue(data['queueRevision']);
      if (previousScope != scope || (wasAuthorized && !authorized)) {
        _parents.clear();
      }
      final encoded = textValue(data['artworkBase64']);
      artwork = encoded.isEmpty || encoded.length > 65536
          ? null
          : base64Decode(encoded);
      error = null;
      notifyListeners();
    } catch (issue) {
      _bridgeFailed(issue);
    }
  }

  void _bridgeFailed(Object issue) {
    if (_disposed) return;
    authorized = false;
    mutationPending = false;
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
        busy ||
        item['browsable'] != true) {
      return;
    }
    final id = textValue(item['id']);
    if (id.isEmpty) return;
    if (!assisted && item['uiAssisted'] != true) {
      _parents.add(currentParent ?? '');
    }
    await invoke('browse', {'parent': id});
  }

  Future<void> back() async {
    if (!canGoBack || !can('canBrowse') || libraryLoading || busy) return;
    final parent = assisted
        ? textValue(library['backId'])
        : _parents.removeLast();
    await invoke('browse', {'parent': parent.isEmpty ? null : parent});
  }

  Future<void> more(int page) async {
    if (!can('canBrowse') || libraryLoading || busy || page < 0) return;
    await invoke('browse', {
      'parent': assisted ? library['parent'] : currentParent,
      'page': page,
    });
  }

  Future<void> playItem(Json item) async {
    if (item['playable'] != true) return;
    await command('PLAY_ITEM', {
      'item': item['id'],
      'revision': library['revision'],
    }, 'canPlayItem');
  }

  Future<void> playQueue(Json item) async {
    await command('PLAY_QUEUE_ITEM', {
      'item': item['id'],
      'revision': queueRevision,
    }, 'canPlayQueueItem');
  }

  @override
  void dispose() {
    _disposed = true;
    _subscription?.cancel();
    super.dispose();
  }
}
