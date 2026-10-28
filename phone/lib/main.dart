import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
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
  late final SemanticsHandle _semantics;
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
    // Keep the small controller's tree available to native accessibility clients,
    // including automation clients that attach after Flutter has drawn a frame.
    _semantics = SemanticsBinding.instance.ensureSemantics();
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
    if (state == AppLifecycleState.resumed) unawaited(remote.invoke('resume'));
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _semantics.dispose();
    _tab.dispose();
    remote.dispose();
    super.dispose();
  }

  void setup() => setState(() => _tab.value = 3);
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
      if (remote.state['duration'] is num)
        label(
          '${time(remote.state['position'])} / ${time(remote.state['duration'])} · source snapshot',
        ),
      if (remote.can('canSeek'))
        action(
          'Back 10 seconds',
          perform(
            remote.action('canSeek'),
            () => remote.command('SEEK', {
              'position':
                  ((remote.state['position'] as num? ?? 0).toInt() - 10000)
                      .clamp(0, 1 << 53),
            }, 'canSeek'),
          ),
        ),
      if (remote.authorized && !remote.can('canSeek'))
        label('Seeking is unavailable for this source.'),
      label('Volume control awaits verification on the DX340 output.'),
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
        textValue(remote.caps['reason']).isEmpty
            ? 'Assisted browsing needs DX340 awake and unlocked.'
            : textValue(remote.caps['reason']),
      ),
    if (remote.authorized && !remote.can('canBrowse'))
      label(
        'Browsing is unavailable. Open the selected music app on DX340, then refresh the player.',
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
          remote.can('canBrowse') && !remote.libraryLoading && !remote.busy,
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
          remote.can('canBrowse') && !remote.libraryLoading && !remote.busy,
          () => remote.more(remote.nextPage!),
        ),
      ),
  ];
  Widget musicRow(Json item, {required bool queue}) {
    final browsable = !queue && item['browsable'] == true;
    final enabled =
        !remote.busy &&
        (browsable
            ? remote.can('canBrowse') && !remote.libraryLoading
            : remote.can(queue ? 'canPlayQueueItem' : 'canPlayItem') &&
                  (queue || item['playable'] == true));
    final callback = perform(
      enabled,
      () => queue
          ? remote.playQueue(item)
          : browsable
          ? remote.open(item)
          : remote.playItem(item),
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
      children: [
        if (apple)
          CupertinoButton(
            onPressed: callback,
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Row(children: [icon, const SizedBox(width: 16), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [title, if (subtitle.isNotEmpty) Text(subtitle, style: Theme.of(context).textTheme.bodyMedium)])), if (browsable) const CupertinoListTileChevron()]),
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
        remote.can('canReadQueue'),
        () => remote.command('GET_QUEUE', {}, 'canReadQueue'),
      ),
      icon: Icons.refresh,
    ),
    if (remote.authorized && remote.can('canReadQueue') && remote.queue.isEmpty)
      label('Refresh to read the queue from DX340.'),
    for (final item in remote.queue) musicRow(item, queue: true),
  ];
  List<Widget> setupView() => [
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
  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: remote,
    builder: (context, child) {
      final content = ListView(
        key: PageStorageKey(_tab.value),
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
        children: [
          Semantics(
            liveRegion: true,
            child: Text(
              remote.status,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ),
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
          const SizedBox(height: 20),
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
                      child: content,
                    ),
                  ),
                  CupertinoTabBar(
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
        body: SafeArea(child: content),
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

String time(dynamic value) {
  final seconds = (value is num ? value.toInt() : 0) ~/ 1000;
  return '${seconds ~/ 60}:${(seconds % 60).toString().padLeft(2, '0')}';
}
