import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'remote.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(DX340App(remote: Remote(NativeBridge())));
}

class DX340App extends StatelessWidget {
  const DX340App({super.key, required this.remote});
  final Remote remote;
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'DX340 Remote',
    debugShowCheckedModeBanner: false,
    restorationScopeId: 'dx340',
    themeMode: ThemeMode.system,
    theme: ThemeData(useMaterial3: true, brightness: Brightness.light),
    darkTheme: ThemeData(useMaterial3: true, brightness: Brightness.dark),
    home: PhoneShell(remote: remote),
  );
}

class PhoneShell extends StatefulWidget {
  const PhoneShell({super.key, required this.remote});
  final Remote remote;
  @override
  State<PhoneShell> createState() => _PhoneShellState();
}

class _PhoneShellState extends State<PhoneShell>
    with WidgetsBindingObserver, RestorationMixin {
  final _tab = RestorableInt(0);
  Remote get remote => widget.remote;
  bool get apple => Theme.of(context).platform == TargetPlatform.iOS;
  static const _tabs = ['Player', 'Library', 'Queue', 'Setup'];
  @override
  String get restorationId => 'phone_shell';
  @override
  void restoreState(RestorationBucket? oldBucket, bool initialRestore) =>
      registerForRestoration(_tab, 'tab');
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    unawaited(remote.start());
    if (kDebugMode) {
      remote.bridge
          .call('captureTab')
          .then((value) {
            final index = _tabs.indexWhere((tab) => tab.toLowerCase() == value);
            if (mounted && index >= 0) setState(() => _tab.value = index);
          })
          .catchError((Object _) {});
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    remote.setForeground(state == AppLifecycleState.resumed);
    if (state == AppLifecycleState.resumed) unawaited(remote.invoke('resume'));
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _tab.dispose();
    remote.dispose();
    super.dispose();
  }

  void setup() => setState(() => _tab.value = 3);
  Widget? browsingPlaybackBar() {
    final title = textValue(remote.state['title']);
    if (!remote.authorized || title.isEmpty || !{1, 2}.contains(_tab.value)) {
      return null;
    }
    final artist = textValue(remote.state['artist']);
    final playing = remote.state['playing'] == true;
    final name = playing ? 'Pause' : 'Play';
    final capability = playing ? 'canPause' : 'canPlay';
    final details = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(title, maxLines: 1, overflow: TextOverflow.ellipsis),
        if (artist.isNotEmpty &&
            MediaQuery.textScalerOf(context).scale(1) <= 1.4)
          Text(artist, maxLines: 1, overflow: TextOverflow.ellipsis),
      ],
    );
    void open() => setState(() => _tab.value = 0);
    final metadata = Semantics(
      label: 'Open Player, $title${artist.isEmpty ? '' : ', $artist'}',
      excludeSemantics: true,
      button: true,
      onTap: open,
      child: apple
          ? CupertinoButton(
              alignment: Alignment.centerLeft,
              onPressed: open,
              child: details,
            )
          : TextButton(
              onPressed: open,
              child: Align(alignment: Alignment.centerLeft, child: details),
            ),
    );
    return DecoratedBox(
      key: const ValueKey('browsing-playback-bar'),
      decoration: BoxDecoration(
        color: apple
            ? CupertinoColors.systemBackground.resolveFrom(context)
            : Theme.of(context).colorScheme.surface,
        border: Border(
          top: BorderSide(
            color: apple
                ? CupertinoColors.separator.resolveFrom(context)
                : Theme.of(context).dividerColor,
          ),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        child: Row(
          children: [
            Expanded(child: metadata),
            const SizedBox(width: 8),
            SizedBox(
              width: 48,
              height: 48,
              child: apple
                  ? CupertinoButton(
                      padding: EdgeInsets.zero,
                      onPressed: perform(
                        remote.action(capability),
                        () => remote.command('SET_PLAYING', {
                          'playing': !playing,
                        }, capability),
                      ),
                      child: Semantics(
                        label: name,
                        child: Icon(playing ? Icons.pause : Icons.play_arrow),
                      ),
                    )
                  : IconButton(
                      tooltip: name,
                      onPressed: perform(
                        remote.action(capability),
                        () => remote.command('SET_PLAYING', {
                          'playing': !playing,
                        }, capability),
                      ),
                      icon: Icon(playing ? Icons.pause : Icons.play_arrow),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget label(String text, {bool heading = false}) => Padding(
    padding: EdgeInsets.only(top: heading ? 24 : 8, bottom: 8),
    child: Semantics(
      header: heading,
      child: Text(
        text,
        style: heading
            ? Theme.of(context).textTheme.titleLarge
            : Theme.of(context).textTheme.bodyLarge,
      ),
    ),
  );
  Widget action(String title, VoidCallback? onPressed, {IconData? icon}) {
    final child = Row(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        if (icon != null) ...[Icon(icon, size: 22), const SizedBox(width: 8)],
        Flexible(child: Text(title, textAlign: TextAlign.center)),
      ],
    );
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: ConstrainedBox(
        constraints: const BoxConstraints(minHeight: 48),
        child: apple
            ? CupertinoButton(onPressed: onPressed, child: child)
            : OutlinedButton(
                onPressed: onPressed,
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  child: child,
                ),
              ),
      ),
    );
  }

  VoidCallback? perform(bool enabled, Future<dynamic> Function() callback) =>
      enabled
      ? () {
          unawaited(callback());
        }
      : null;
  Widget recovery(String purpose) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      label('Connect and approve this phone in Setup to $purpose.'),
      action('Open Setup', setup, icon: Icons.bluetooth),
    ],
  );
  List<Widget> player() {
    final title = textValue(remote.state['title']);
    final artist = textValue(remote.state['artist']);
    final album = textValue(remote.state['album']);
    return [
      if (remote.artwork != null)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 16),
          child: Image.memory(
            remote.artwork!,
            height: 220,
            fit: BoxFit.contain,
            semanticLabel: 'Album artwork',
            errorBuilder: (context, error, stack) => const SizedBox.shrink(),
          ),
        ),
      Semantics(
        header: true,
        child: Text(
          title.isEmpty ? 'No track connected' : title,
          style: Theme.of(context).textTheme.headlineSmall,
        ),
      ),
      if (artist.isNotEmpty) label(artist),
      if (album.isNotEmpty) label(album),
      if (!remote.authorized) recovery('control playback'),
      if (remote.authorized &&
          remote.state['accessError'] == 'NOTIFICATION_ACCESS_REQUIRED')
        label(
          'On DX340, enable playback session access in Notification access settings, then refresh the player.',
        ),
      const SizedBox(height: 16),
      LayoutBuilder(
        builder: (context, bounds) {
          final stacked =
              bounds.maxWidth < 340 ||
              MediaQuery.textScalerOf(context).scale(1) > 1.4;
          final controls = [
            action(
              'Play',
              perform(
                remote.action('canPlay'),
                () =>
                    remote.command('SET_PLAYING', {'playing': true}, 'canPlay'),
              ),
              icon: Icons.play_arrow,
            ),
            action(
              'Pause',
              perform(
                remote.action('canPause'),
                () => remote.command('SET_PLAYING', {
                  'playing': false,
                }, 'canPause'),
              ),
              icon: Icons.pause,
            ),
            action(
              'Previous',
              perform(
                remote.action('canPrevious'),
                () => remote.command('PREVIOUS', {}, 'canPrevious'),
              ),
              icon: Icons.skip_previous,
            ),
            action(
              'Next',
              perform(
                remote.action('canNext'),
                () => remote.command('NEXT', {}, 'canNext'),
              ),
              icon: Icons.skip_next,
            ),
          ];
          if (stacked) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: controls,
            );
          }
          return Column(
            children: [
              for (var i = 0; i < 4; i += 2)
                Row(
                  children: [
                    Expanded(child: controls[i]),
                    const SizedBox(width: 12),
                    Expanded(child: controls[i + 1]),
                  ],
                ),
            ],
          );
        },
      ),
      if (remote.progress.durationMs != null) ElapsedTime(remote: remote),
      if (remote.can('canSeek'))
        action(
          'Back 10 seconds',
          perform(
            remote.action('canSeek') && remote.progress.positionMs != null,
            () => remote.command('SEEK', {
              'position': ((remote.progress.positionMs ?? 0) - 10000).clamp(
                0,
                1 << 53,
              ),
            }, 'canSeek'),
          ),
        ),
      if (remote.authorized && !remote.can('canSeek'))
        label('Seeking is unavailable for this source.'),
      label('Volume', heading: true),
      if (remote.can('canAdjustVolume') &&
          remote.volume['verified'] == true) ...[
        label(
          'Level ${remote.volume['current']} · ${textValue(remote.volume['outputLabel'])}',
        ),
        label(
          'Confirmed range ${remote.volume['verifiedMinimum']}–${remote.volume['verifiedMaximum']}. Recheck on DX340 after changing the output or gain.',
        ),
        action(
          'Volume down',
          perform(
            remote.volumeStepAvailable(-1),
            () => remote.adjustVolume(-1),
          ),
          icon: Icons.volume_down,
        ),
        action(
          'Volume up',
          perform(remote.volumeStepAvailable(1), () => remote.adjustVolume(1)),
          icon: Icons.volume_up,
        ),
      ] else
        label(
          remote.authorized && textValue(remote.volume['reason']).isNotEmpty
              ? textValue(remote.volume['reason'])
              : 'Verify this source and output in the DX340 companion before using phone volume.',
        ),
      action(
        'Refresh player',
        perform(remote.authorized, () => remote.command('GET_STATE')),
        icon: Icons.refresh,
      ),
      if (remote.sources.isNotEmpty) label('Source', heading: true),
      for (final source in remote.sources)
        action(
          'Control ${textValue(source['name'])}${source['id'] == remote.state['source'] ? ' · selected' : ''}',
          perform(
            remote.authorized && !remote.busy && source['installed'] != false,
            () => remote.command('SELECT_SOURCE', {'source': source['id']}),
          ),
        ),
    ];
  }

  List<Widget> library() => [
    if (!remote.authorized) recovery('browse music on DX340'),
    if (remote.authorized && remote.assisted)
      label(
        remote.browseExplanation.isEmpty
            ? 'Assisted browsing needs DX340 awake and unlocked.'
            : remote.browseExplanation,
      ),
    if (remote.authorized && !remote.can('canBrowse'))
      label(
        remote.browseExplanation.isNotEmpty
            ? remote.browseExplanation
            : 'Browsing is unavailable. Open the selected music app on DX340, then refresh the player.',
      ),
    action(
      'Browse source library',
      perform(
        remote.can('canBrowse') && !remote.libraryLoading && !remote.busy,
        remote.root,
      ),
      icon: Icons.library_music,
    ),
    if (remote.libraryLoading)
      const Padding(
        padding: EdgeInsets.all(16),
        child: Center(
          child: CircularProgressIndicator.adaptive(
            semanticsLabel: 'Loading library',
          ),
        ),
      ),
    if (remote.canGoBack)
      action(
        'Back to parent',
        perform(
          remote.can('canBrowse') &&
              !remote.libraryLoading &&
              !remote.busy &&
              (!remote.assisted || remote.assistedReady),
          remote.back,
        ),
        icon: Icons.arrow_back,
      ),
    if (textValue(remote.library['title']).isNotEmpty)
      label(textValue(remote.library['title']), heading: true),
    for (final item in objects(remote.library['items']))
      musicRow(item, queue: false),
    if (remote.library.containsKey('items') &&
        objects(remote.library['items']).isEmpty &&
        !remote.libraryLoading)
      label('No items were returned for this page.'),
    if (!remote.assisted && remote.page > 0)
      action(
        'Previous page',
        perform(
          remote.can('canBrowse') && !remote.libraryLoading && !remote.busy,
          () => remote.more(remote.page - 1),
        ),
      ),
    if (remote.nextPage != null)
      action(
        remote.assisted ? 'Show more' : 'Next page',
        perform(
          remote.can('canBrowse') &&
              !remote.libraryLoading &&
              !remote.busy &&
              (!remote.assisted || remote.assistedReady),
          () => remote.more(remote.nextPage!),
        ),
      ),
  ];
  Widget musicRow(Json item, {required bool queue}) {
    final queueRevision = remote.queueRevision;
    final libraryRevision = textValue(remote.library['revision']);
    final sourceScope = remote.scope;
    final browsable = !queue && item['browsable'] == true;
    final enabled =
        !remote.busy &&
        (queue ||
            (!(remote.assisted || item['uiAssisted'] == true) ||
                remote.assistedReady)) &&
        (!queue || !remote.queueLoading) &&
        (browsable
            ? remote.can('canBrowse') && !remote.libraryLoading
            : remote.can(queue ? 'canPlayQueueItem' : 'canPlayItem') &&
                  (queue || item['playable'] == true));
    final callback = perform(
      enabled,
      () => queue
          ? remote.playQueue(item, revision: queueRevision)
          : browsable
          ? remote.open(item)
          : remote.playItem(
              item,
              revision: libraryRevision,
              sourceScope: sourceScope,
            ),
    );
    final title = Text(
      textValue(item['title']).isEmpty ? 'Untitled' : textValue(item['title']),
    );
    final subtitle = textValue(item['subtitle']);
    final icon = Icon(
      browsable ? Icons.folder_outlined : Icons.music_note,
      semanticLabel: browsable ? 'Browse' : 'Play',
    );
    return Column(
      key: ValueKey((
        queue,
        sourceScope,
        queue ? queueRevision : libraryRevision,
        item['id'],
      )),
      children: [
        if (apple)
          CupertinoButton(
            onPressed: callback,
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Row(
              children: [
                icon,
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      title,
                      if (subtitle.isNotEmpty)
                        Text(
                          subtitle,
                          style: Theme.of(context).textTheme.bodyMedium,
                        ),
                    ],
                  ),
                ),
                if (browsable) const CupertinoListTileChevron(),
              ],
            ),
          )
        else
          ListTile(
            enabled: enabled,
            contentPadding: EdgeInsets.zero,
            title: title,
            subtitle: subtitle.isEmpty ? null : Text(subtitle),
            leading: icon,
            trailing: browsable ? const Icon(Icons.chevron_right) : null,
            onTap: callback,
            minVerticalPadding: 12,
          ),
        const Divider(height: 1),
      ],
    );
  }

  List<Widget> queueView() => [
    if (!remote.authorized) recovery('view the playback queue'),
    if (remote.authorized && !remote.can('canReadQueue'))
      label('This source has not exposed a usable queue.'),
    action(
      'Refresh queue',
      perform(
        remote.can('canReadQueue') && !remote.queueLoading,
        () => remote.readQueue(),
      ),
      icon: Icons.refresh,
    ),
    if (remote.queueLoading) label('Loading queue…'),
    if (remote.authorized &&
        remote.can('canReadQueue') &&
        remote.queue.isEmpty &&
        !remote.queueLoading)
      label(
        remote.queueTotal == 0
            ? 'The queue is empty.'
            : 'Refresh to read the queue from DX340.',
      ),
    if (remote.queue.isNotEmpty && remote.queueTotal != null)
      label(
        'Queue items ${remote.queueOffset + 1}–${remote.queueOffset + remote.queue.length} of ${remote.queueTotal}',
      ),
    for (final item in remote.queue) musicRow(item, queue: true),
    if (remote.queueOffset > 0)
      action(
        'Start of queue',
        perform(
          remote.can('canReadQueue') && !remote.queueLoading,
          () => remote.readQueue(),
        ),
      ),
    if (remote.queueNextOffset != null)
      action(
        'Next queue page',
        perform(
          remote.can('canReadQueue') && !remote.queueLoading,
          () => remote.readQueue(remote.queueNextOffset!),
        ),
      ),
  ];
  List<Widget> setupView() => [
    if (remote.controlRequestAvailable) ...[
      action(
        'Take control on this phone',
        perform(remote.canRequestControl, remote.requestControl),
      ),
      label(
        'Taking control here disconnects the other approved controller. Music keeps playing on DX340.',
      ),
    ],
    label(
      'Music plays on DX340. This phone sends controls directly over Bluetooth.',
    ),
    label(
      'Start the companion on DX340 and open pairing for a new phone. Accept the system pairing request, then approve this phone on DX340.',
    ),
    action(
      apple ? 'Check Bluetooth access' : 'Allow Nearby Devices access',
      perform(true, () => remote.requestPermissions(apple: apple)),
      icon: Icons.bluetooth,
    ),
    action(
      'Find DX340',
      perform(true, () => remote.invoke('scan')),
      icon: Icons.search,
    ),
    for (final device in remote.devices)
      action(
        'Connect to ${textValue(device['name'])} · ${textValue(device['id']).split('-').last.substring(0, textValue(device['id']).split('-').last.length.clamp(0, 5))}',
        perform(true, () => remote.invoke('connect', {'id': device['id']})),
      ),
    action(
      'Retry authorization after approval',
      perform(true, () => remote.invoke('authorize')),
    ),
    action('Disconnect', perform(true, () => remote.invoke('disconnect'))),
    label(
      'Disconnecting leaves music playing on DX340. Cached titles and lists may be stale until this phone reconnects.',
    ),
    label(
      'Optional assisted browsing needs DX340 awake and unlocked. Enable it in the companion only when you want that mode.',
    ),
  ];
  Widget statusContent() => Semantics(
    liveRegion: true,
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(remote.status, style: Theme.of(context).textTheme.bodyMedium),
        if (remote.error != null)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Text(
              remote.error!,
              style: TextStyle(
                color: apple
                    ? CupertinoColors.systemRed.resolveFrom(context)
                    : Theme.of(context).colorScheme.error,
              ),
            ),
          ),
        if (remote.busy)
          label(
            'Waiting for the playback action. Refresh if its outcome is unknown.',
          ),
      ],
    ),
  );

  // Status can change while a finger is over a song. Give it its own scrolling
  // viewport, so wrapping text and pending messages never reflow the music list.
  // The viewport scales with text, remains bounded on short displays and never
  // truncates the recovery message; its content can be scrolled independently.
  Widget stableBrowsingContent(Widget content) => LayoutBuilder(
    builder: (context, bounds) {
      final style = Theme.of(context).textTheme.bodyMedium!;
      final height =
          (MediaQuery.textScalerOf(context).scale(style.fontSize!) *
                      (style.height ?? 1.5) *
                      3 +
                  24)
              .clamp(0.0, bounds.maxHeight * 0.35)
              .toDouble();
      return Column(
        children: [
          SizedBox(
            height: height,
            child: SingleChildScrollView(
              key: ValueKey((remote.status, remote.error, remote.busy)),
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              child: statusContent(),
            ),
          ),
          const Divider(height: 1),
          Expanded(child: content),
        ],
      );
    },
  );
  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: remote,
    builder: (context, child) {
      final browsing = {1, 2}.contains(_tab.value);
      // A tab alone is not a browse location. Inheriting another folder's saved
      // offset can put a different song under a finger when a fast page arrives.
      final Object scrollContext = switch (_tab.value) {
        1 => (
          'library',
          remote.scope,
          textValue(remote.library['revision']),
          remote.currentParent,
          textValue(remote.library['parent']),
          remote.page,
          remote.libraryLoading,
        ),
        2 => ('queue', remote.scope, remote.queueRevision, remote.queueOffset),
        _ => ('tab', _tab.value),
      };
      final content = ListView(
        key: PageStorageKey(scrollContext),
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
        children: [
          if (!browsing) ...[statusContent(), const SizedBox(height: 20)],
          ...switch (_tab.value) {
            1 => library(),
            2 => queueView(),
            3 => setupView(),
            _ => player(),
          },
        ],
      );
      const icons = [
        Icons.play_circle_outline,
        Icons.library_music_outlined,
        Icons.queue_music,
        Icons.bluetooth,
      ];
      final playbackBar = browsingPlaybackBar();
      if (apple) {
        return CupertinoTheme(
          data: CupertinoThemeData(brightness: Theme.of(context).brightness),
          child: CupertinoPageScaffold(
            navigationBar: CupertinoNavigationBar(
              middle: Text(_tab.value == 0 ? 'DX340' : _tabs[_tab.value]),
            ),
            child: SafeArea(
              bottom: false,
              child: Column(
                children: [
                  Expanded(
                    child: Material(
                      type: MaterialType.transparency,
                      child: browsing
                          ? stableBrowsingContent(content)
                          : content,
                    ),
                  ),
                  ?playbackBar,
                  // CupertinoTabScaffold normally supplies this chrome behavior;
                  // this shared shell composes the same fixed-height bar directly.
                  MediaQuery.withNoTextScaling(
                    child: CupertinoTabBar(
                      currentIndex: _tab.value,
                      onTap: (value) => setState(() => _tab.value = value),
                      items: [
                        for (var i = 0; i < 4; i++)
                          BottomNavigationBarItem(
                            icon: Icon(icons[i]),
                            label: _tabs[i],
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      }
      return Scaffold(
        appBar: AppBar(
          title: Text(_tab.value == 0 ? 'DX340' : _tabs[_tab.value]),
        ),
        body: SafeArea(
          child: Column(
            children: [
              Expanded(
                child: browsing ? stableBrowsingContent(content) : content,
              ),
              ?playbackBar,
            ],
          ),
        ),
        bottomNavigationBar: NavigationBar(
          selectedIndex: _tab.value,
          onDestinationSelected: (value) => setState(() => _tab.value = value),
          destinations: [
            for (var i = 0; i < 4; i++)
              NavigationDestination(icon: Icon(icons[i]), label: _tabs[i]),
          ],
        ),
      );
    },
  );
}

/// Only this text rebuilds each second; hidden tabs and background apps do no work.
class ElapsedTime extends StatefulWidget {
  const ElapsedTime({super.key, required this.remote});
  final Remote remote;
  @override
  State<ElapsedTime> createState() => _ElapsedTimeState();
}

class _ElapsedTimeState extends State<ElapsedTime> {
  Timer? _timer;
  @override
  void initState() {
    super.initState();
    widget.remote.addListener(_remoteChanged);
    _syncTimer();
  }

  @override
  void didUpdateWidget(ElapsedTime oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.remote != widget.remote) {
      oldWidget.remote.removeListener(_remoteChanged);
      widget.remote.addListener(_remoteChanged);
    }
    _syncTimer();
  }

  void _remoteChanged() {
    // Lifecycle changes can disable frames before a parent rebuild occurs.
    // Cancel the timer immediately when the model freezes its estimate.
    _syncTimer();
    if (mounted) setState(() {});
  }

  void _syncTimer() {
    if (!widget.remote.progress.ticking) {
      _timer?.cancel();
      _timer = null;
    } else {
      _timer ??= Timer.periodic(const Duration(seconds: 1), (_) {
        _syncTimer();
        if (mounted) setState(() {});
      });
    }
  }

  @override
  void dispose() {
    widget.remote.removeListener(_remoteChanged);
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final progress = widget.remote.progress;
    final kind = !widget.remote.authorized
        ? 'cached'
        : progress.needsRefresh
        ? 'refresh for current time'
        : progress.estimated
        ? 'estimated'
        : 'source snapshot';
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Text(
        '${time(progress.positionMs)} / ${time(progress.durationMs)} · $kind',
        // A running clock should not interrupt screen-reader speech each second.
        semanticsLabel:
            'Elapsed ${time(progress.positionMs)} of ${time(progress.durationMs)}, $kind',
      ),
    );
  }
}

String time(dynamic value) {
  if (value is! num || !value.isFinite || value < 0) return '--:--';
  final seconds = value.toInt() ~/ 1000;
  return '${seconds ~/ 60}:${(seconds % 60).toString().padLeft(2, '0')}';
}
