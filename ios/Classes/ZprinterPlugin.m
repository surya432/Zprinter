#import "ZprinterPlugin.h"
#if __has_include(<Zprinter/Zprinter-Swift.h>)
#import <Zprinter/Zprinter-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "Zprinter-Swift.h"
#endif

@implementation ZprinterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftZprinterPlugin registerWithRegistrar:registrar];
}
@end
