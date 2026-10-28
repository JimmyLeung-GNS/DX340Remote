"""Generate the minimal native Xcode project deterministically, without Mac-only dependencies."""
from pathlib import Path
import hashlib
import plistlib

root = Path(__file__).resolve().parents[1] / 'apple'
project = root / 'DXRemote.xcodeproj'
project.mkdir(exist_ok=True)
def uid(name):
    return hashlib.sha1(name.encode()).hexdigest()[:24].upper()
sources = sorted(str(path.relative_to(root)).replace('\\', '/') for folder in ['DXRemote', 'BLEClient', 'ProtocolModels'] for path in (root / folder).glob('*.swift'))
objects = []
def obj(name, content):
    objects.append(f'{uid(name)} = {{ {content} }};')
for path in sources:
    obj('file:' + path, f'isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = "{path}"; sourceTree = "<group>";')
    obj('build:' + path, f'isa = PBXBuildFile; fileRef = {uid("file:" + path)};')
obj('product', 'isa = PBXFileReference; explicitFileType = wrapper.application; path = DXRemote.app; sourceTree = BUILT_PRODUCTS_DIR;')
obj('products', f'isa = PBXGroup; children = ({uid("product")},); name = Products; sourceTree = "<group>";')
obj('group', f'isa = PBXGroup; children = ({",".join(uid("file:" + p) for p in sources)},{uid("products")},); sourceTree = "<group>";')
obj('sources', f'isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = ({",".join(uid("build:" + p) for p in sources)},); runOnlyForDeploymentPostprocessing = 0;')
obj('frameworks', 'isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0;')
obj('resources', 'isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0;')
for config in ['Debug', 'Release']:
    obj('project:' + config, f'isa = XCBuildConfiguration; name = {config}; buildSettings = {{ SDKROOT = iphoneos; IPHONEOS_DEPLOYMENT_TARGET = 17.0; CLANG_ENABLE_MODULES = YES; SWIFT_VERSION = 5.0; SWIFT_ACTIVE_COMPILATION_CONDITIONS = "{"DEBUG" if config == "Debug" else ""}"; }};')
    obj('target:' + config, f'''isa = XCBuildConfiguration; name = {config}; buildSettings = {{ PRODUCT_BUNDLE_IDENTIFIER = org.dx340.remote.ios; PRODUCT_NAME = "$(TARGET_NAME)"; INFOPLIST_FILE = DXRemote/Info.plist; GENERATE_INFOPLIST_FILE = NO; TARGETED_DEVICE_FAMILY = 1; SUPPORTED_PLATFORMS = "iphoneos iphonesimulator"; CODE_SIGN_STYLE = Automatic; MARKETING_VERSION = 0.1.0; CURRENT_PROJECT_VERSION = 1; SWIFT_OPTIMIZATION_LEVEL = "{'-Onone' if config == 'Debug' else '-O'}"; LD_RUNPATH_SEARCH_PATHS = "$(inherited) @executable_path/Frameworks"; }};''')
for name in ['project', 'target']:
    obj(name + 'Config', f'isa = XCConfigurationList; buildConfigurations = ({uid(name + ":Debug")},{uid(name + ":Release")},); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release;')
obj('target', f'isa = PBXNativeTarget; buildConfigurationList = {uid("targetConfig")}; buildPhases = ({uid("sources")},{uid("frameworks")},{uid("resources")},); buildRules = (); dependencies = (); name = DXRemote; productName = DXRemote; productReference = {uid("product")}; productType = "com.apple.product-type.application";')
obj('project', f'isa = PBXProject; attributes = {{ LastUpgradeCheck = 1600; }}; buildConfigurationList = {uid("projectConfig")}; compatibilityVersion = "Xcode 14.0"; developmentRegion = en; knownRegions = (en, Base,); mainGroup = {uid("group")}; productRefGroup = {uid("products")}; projectDirPath = ""; projectRoot = ""; targets = ({uid("target")},);')
(project / 'project.pbxproj').write_text('// !$*UTF8*$!\n{ archiveVersion = 1; classes = {}; objectVersion = 56; objects = {\n' + '\n'.join(objects) + f'\n}}; rootObject = {uid("project")}; }}\n', encoding='utf-8')
info = dict(CFBundleDevelopmentRegion='en', CFBundleExecutable='$(EXECUTABLE_NAME)', CFBundleIdentifier='$(PRODUCT_BUNDLE_IDENTIFIER)', CFBundleInfoDictionaryVersion='6.0', CFBundleName='$(PRODUCT_NAME)', CFBundleDisplayName='DX340 Remote', CFBundlePackageType='APPL', CFBundleShortVersionString='$(MARKETING_VERSION)', CFBundleVersion='$(CURRENT_PROJECT_VERSION)', LSRequiresIPhoneOS=True, NSBluetoothAlwaysUsageDescription='Connect directly to your DX340 to browse and control its music over Bluetooth.', UIBackgroundModes=['bluetooth-central'], UILaunchScreen={}, UISupportedInterfaceOrientations=['UIInterfaceOrientationPortrait'])
(root / 'DXRemote/Info.plist').write_bytes(plistlib.dumps(info))
schemes = project / 'xcshareddata/xcschemes'
schemes.mkdir(parents=True, exist_ok=True)
reference = f'<BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{uid("target")}" BuildableName="DXRemote.app" BlueprintName="DXRemote" ReferencedContainer="container:DXRemote.xcodeproj"/>'
(schemes / 'DXRemote.xcscheme').write_text(f'''<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion="1600" version="1.3">
<BuildAction parallelizeBuildables="YES" buildImplicitDependencies="YES"><BuildActionEntries><BuildActionEntry buildForTesting="YES" buildForRunning="YES" buildForProfiling="YES" buildForArchiving="YES" buildForAnalyzing="YES">{reference}</BuildActionEntry></BuildActionEntries></BuildAction>
<LaunchAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.IDEFoundation.Launcher.LLDB" launchStyle="0" useCustomWorkingDirectory="NO" ignoresPersistentStateOnLaunch="NO" debugDocumentVersioning="YES" debugServiceExtension="internal" allowLocationSimulation="YES"><BuildableProductRunnable runnableDebuggingMode="0">{reference}</BuildableProductRunnable></LaunchAction>
<ProfileAction buildConfiguration="Release" shouldUseLaunchSchemeArgsEnv="YES" savedToolIdentifier="" useCustomWorkingDirectory="NO" debugDocumentVersioning="YES"><BuildableProductRunnable runnableDebuggingMode="0">{reference}</BuildableProductRunnable></ProfileAction>
<AnalyzeAction buildConfiguration="Debug"/><ArchiveAction buildConfiguration="Release" revealArchiveInOrganizer="YES"/>
</Scheme>''', encoding='utf-8')
