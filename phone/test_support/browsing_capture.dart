// Separate debug-only presentation target; never sends Bluetooth commands.
import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:dx340_phone/main.dart';
import 'package:dx340_phone/remote.dart';

Future<void> main() async {
  if (!kDebugMode) {
    throw StateError('Synthetic captures require a debug build.');
  }
  WidgetsFlutterBinding.ensureInitialized();
  final mode = textValue(await NativeBridge().call('captureTab'));
  runApp(
    DX340App(
      remote: Remote(
        BrowsingCaptureBridge(
          queue: mode.contains('queue'),
          expired: mode.contains('expired'),
        ),
      ),
    ),
  );
}

class BrowsingCaptureBridge implements RemoteBridge {
  BrowsingCaptureBridge({required this.queue, this.expired = false});
  final bool queue;
  final bool expired;
  @override
  Stream<dynamic> get events => const Stream.empty();
  @override
  Future<dynamic> call(String method, [Json args = const {}]) async {
    if (method == 'captureTab') return queue ? 'queue' : 'library';
    if (method == 'resume') return null;
    if (method != 'snapshot') {
      throw StateError('Synthetic capture: Bluetooth actions disabled.');
    }
    return {
      'bridgeVersion': 1,
      'status': 'Synthetic browsing UI · no Bluetooth connection',
      'authorized': true,
      'mutationPending': false,
      'state': {
        'source': 'synthetic-mango',
        'sourceEpoch': 'fixture',
        'title': 'A long synthetic track title for browsing playback controls',
        'artist': 'Synthetic artist',
        'playing': !queue,
        'capabilities': {
          'canPlay': true,
          'canPause': true,
          'canBrowse': true,
          'canReadQueue': true,
        },
      },
      'library': {
        if (expired) 'invalidated': true,
        if (expired) 'uiAssisted': true,
        if (expired) 'backId': 'expired-back',
        'title': 'Synthetic album',
        'revision': 'fixture',
        'scopeSource': 'synthetic-mango',
        'scopeEpoch': 'fixture',
        'items': [
          for (var i = 1; i <= 8; i++)
            {
              'id': 'fixture-$i',
              'title': 'Synthetic track $i',
              'subtitle': 'Presentation fixture',
              'playable': false,
              'browsable': false,
            },
        ],
      },
      'queue': [
        for (var i = 1; i <= 8; i++)
          {
            'id': 'fixture-$i',
            'title': 'Synthetic queued track $i',
            'artist': 'Presentation fixture',
          },
      ],
    };
  }
}
