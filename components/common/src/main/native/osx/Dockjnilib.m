#include <JavaVM/jni.h>
#include <Cocoa/Cocoa.h>

// compile with:
// cc -c -dynamiclib -o libDock.ppc -I/System/Library/Frameworks/JavaVM.framework/Headers Dockjnilib.m -arch ppc
// cc -c -dynamiclib -o libDock.i386 -I/System/Library/Frameworks/JavaVM.framework/Headers Dockjnilib.m -arch i386
// cc -dynamiclib -o libDock.jnilib libDock.ppc libDock.i386 -framework JavaVM -framework Carbon -framework Cocoa -arch ppc -arch i386

#define ICON_WIDTH 128
#define ICON_HEIGHT 128

#ifdef __cplusplus
extern "C" {
#endif

#define OS_NATIVE(func) Java_org_limewire_ui_swing_dock_Dock_##func

@interface Dock : NSObject
+ (NSImage *)limeWireIconAtPath:(NSString *)appPath;
@end
	
@implementation Dock
+ (NSImage *)limeWireIconAtPath:(NSString *)appPath
{
	NSBundle *bundle = [NSBundle bundleWithPath:appPath];
	NSString *iconPath = [bundle pathForResource:@"LimeWire" ofType:@"icns"];
	
	return [[[NSImage alloc] initWithContentsOfFile:iconPath] autorelease];
}
@end

JNIEXPORT jint JNICALL OS_NATIVE(RequestUserAttention)
	(JNIEnv *env, jobject clazz, jint requestType)
{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	jint request = (jint)[[NSApplication sharedApplication] 
					requestUserAttention: (NSRequestUserAttentionType)requestType];
	[pool release];
	return request;
}

JNIEXPORT void JNICALL OS_NATIVE(CancelUserAttentionRequest)
	(JNIEnv *env, jobject clazz, jint request)
{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	[[NSApplication sharedApplication] cancelUserAttentionRequest: request];
	[pool release];
}

JNIEXPORT void JNICALL OS_NATIVE(RestoreApplicationDockTileImage)
	(JNIEnv *env, jobject clazz, jstring appdir)
{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	const char *appdircstr = (*env)->GetStringUTFChars(env, appdir, NULL);
	NSImage *newImage = [Dock limeWireIconAtPath:[NSString stringWithUTF8String:appdircstr]];
	[NSApp setApplicationIconImage:newImage];
	
	(*env)->ReleaseStringUTFChars(env, appdir, appdircstr);
	[pool release];
}

/**
* This is a custom view for painting the icon for the application in the dock.
* It's necessary to create a custom view in order to do this to ensure that
* you have a valid graphics context object to paint with. An instance of
* this object should be set to NSApp's dockTile and display should be called
* on dockTile. Then, this View's drawRect() method will be called by the main
* thread with a valid graphics context.
*/
@interface ViewForDrawingDockIcon : NSView
{
    int* iconData;
    int iconDataSize;
    NSString* applicationDirectory;
    bool overlay;
    NSLock* iconDataLock;

}

- (id) init: (NSString*) applicationDirectory; 
- (void) dealloc; 
- (int*) getIconData;
- (int) getIconDataSize;
- (void) lockIconData;
- (void) unlockIconData;
- (void) setApplicationDirectory: (NSString*) argApplicationDirectory;
- (void) setOverlay: (bool) argOverlay;
- (void) drawRect: (NSRect) argDirtyRectangle;
@end

@implementation ViewForDrawingDockIcon
- (id) init: (NSString*) argApplicationDirectory {
    applicationDirectory = argApplicationDirectory;
    iconDataLock = [[NSLock alloc] init];
    iconDataSize = ICON_WIDTH * ICON_HEIGHT * 4;
    iconData = (int*) malloc(iconDataSize);
    if ( iconData == NULL ) {
        // Java out of memory exception should already have been thrown
    }
    overlay = false;

    return [super init];
}

- (int) getIconDataSize {
    return iconDataSize;
}

- (void) dealloc {
    if (iconData != nil) {
        free( (void*) iconData);
        iconData = nil;
    }

    if (applicationDirectory != nil) {
        [applicationDirectory release];
    }

    [iconDataLock release];
    
    [super dealloc];
}

- (const int*) getIconData {
    return iconData;
}

- (void) lockIconData {
    [iconDataLock lock];
}

- (void) unlockIconData {
    [iconDataLock unlock];
}

- (void) setApplicationDirectory: (NSString*) argApplicationDirectory {
    if (applicationDirectory != nil) {
        [applicationDirectory release];
    }

    applicationDirectory = argApplicationDirectory;
}

- (void) setOverlay: (bool) argOverlay {
    overlay = argOverlay;
}

- (void) drawRect: (NSRect) argDirtyRectangle {
    
    [self lockIconData];
    
    CGDataProviderRef provider = CGDataProviderCreateWithData(0, iconData, iconDataSize, 0);
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    CGImageRef image = CGImageCreate(ICON_WIDTH, 
                                     ICON_HEIGHT, 
                                     8, 
                                     32, 
                                     ICON_WIDTH*4, 
                                     cs, 
                                     kCGBitmapByteOrder32Host | kCGImageAlphaFirst, 
                                     provider, 
                                     NULL, 
                                     0, 
                                     kCGRenderingIntentDefault);

    NSImage* newImage = [Dock limeWireIconAtPath:applicationDirectory];
    
    NSRect imageRect = NSMakeRect(0.0, 0.0, 0.0, 0.0);
    
    // Create a new image to receive the Quartz image data.
    imageRect.size.height = [newImage size].height;
    imageRect.size.width = [newImage size].width;    
    
    // if overlay == true
    //   composite image to app icon
    //   set to new application icon
    // else
    //   composite application icon to blank image
    //   set to new application icon
    if (overlay) { 
        // Get the Quartz context and draw.
        [newImage lockFocus];
        CGContextRef imageContext = (CGContextRef)[[NSGraphicsContext currentContext] graphicsPort];
        CGContextDrawImage(imageContext, *(CGRect*)&imageRect, image);
        [newImage unlockFocus];
    }

    [NSApp setApplicationIconImage:newImage];

    [self unlockIconData];

    CFRelease(cs);
    CGDataProviderRelease(provider);
    CGImageRelease(image);
}
@end

ViewForDrawingDockIcon* customDockIconView = nil;

JNIEXPORT void JNICALL OS_NATIVE(DrawDockTileImage)
    (JNIEnv *env, jobject clazz, jintArray pixels, jboolean overlay, jstring appdir)
{
    // When the application first starts, we want to create a custom view for setting images
    // to the dock, and we want to set it as the application's dock tile's view.
    if ( [[NSApp dockTile] contentView] != customDockIconView ) {
        // Convert the Java string for the application directory into an NSString object.
        // Calling GetStringChars() preserves the string in UTF-16. So, no i18n information is lost.
        const jchar *applicationDirectoryChars = (*env)->GetStringChars(env, appdir, NULL);
        NSString* applicationDirectoryNSString = [NSString stringWithCharacters:(UniChar *)applicationDirectoryChars
                                                  length:(*env)->GetStringLength(env, appdir)];
        (*env)->ReleaseStringChars(env, appdir, applicationDirectoryChars);

        customDockIconView = [[[ViewForDrawingDockIcon alloc] init:applicationDirectoryNSString] autorelease]; // Obj-C class
        
        [[NSApp dockTile] setContentView:customDockIconView];
    }

    // we need to free the image data when this method returns, but we still need
    // a copy of the data afterward in our view object. so, let's copy the
    // image data into an array that our custom view owns.
    jint* newIconData = (*env)->GetIntArrayElements(env, pixels, 0);

    [customDockIconView lockIconData];
    int* iconData = [customDockIconView getIconData];
    if (iconData != NULL) {
        memcpy(iconData, newIconData, [customDockIconView getIconDataSize]);
    }
    [customDockIconView unlockIconData];

    (*env)->ReleaseIntArrayElements(env, pixels, newIconData, 0);

    // let's set whether the image is an overlay or not.
    [customDockIconView setOverlay:overlay];

    // Call display on the application's dock tile object. This will cause the 
    // drawRect() method in our custom view to be called with a valid drawing context.
    // This is analagous to calling repaint() in Java which will cause paint() to 
    // be called with a valid Graphics object.
    [[NSApp dockTile] display];
}

#ifdef __cplusplus
}
#endif
