//
//  MacOSXUtils.m
//  GURL
//
//  Created by Curtis Jones on 2008.04.08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//

#import <JavaVM/jni.h>
#import <Foundation/Foundation.h>
#import <Cocoa/Cocoa.h>

#ifdef __cplusplus
extern "C" {
#endif

#define OS_NATIVE(func) Java_org_limewire_ui_swing_util_MacOSXUtils_##func

JNIEXPORT jstring JNICALL OS_NATIVE(GetCurrentFullUserName)
    (JNIEnv *env, jobject clazz)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSString *usernameNSString = NSFullUserName();
    jstring usernameJString = (*env)->NewStringUTF(env, [usernameNSString UTF8String]);
    [pool release];
    
    return usernameJString;
}

JNIEXPORT void JNICALL OS_NATIVE(SetLoginStatusNative)
    (JNIEnv *env, jobject obj, jboolean onoff)
{
    NSMutableArray *loginItems;
    NSDictionary *appDict;
    NSEnumerator *appEnum;
    NSString *agentAppPath = @"/Applications/LimeWire.app";
    
    NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];

    // Make a mutable copy (why a copy?)
    loginItems = (NSMutableArray *)CFPreferencesCopyValue((CFStringRef)@"AutoLaunchedApplicationDictionary", 
                                                                                                                (CFStringRef)@"loginwindow", kCFPreferencesCurrentUser, 
                                                                                                                kCFPreferencesAnyHost);
    loginItems = [[loginItems autorelease] mutableCopy];
    appEnum = [loginItems objectEnumerator];
    
    while ((appDict = [appEnum nextObject])) {
        if ([[[appDict objectForKey:@"Path"] stringByExpandingTildeInPath] isEqualToString:agentAppPath])
            break;
    }
    
    // register the item
    if (onoff == JNI_TRUE) {
        if (!appDict)
            [loginItems addObject:[NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithBool:NO], @"Hide", agentAppPath, @"Path", nil]];
    }
    // unregister the item
    else if (appDict)
        [loginItems removeObject:appDict];
    
    CFPreferencesSetValue((CFStringRef)@"AutoLaunchedApplicationDictionary", 
                                                loginItems, (CFStringRef)@"loginwindow", 
                                                kCFPreferencesCurrentUser, kCFPreferencesAnyHost);
    
    CFPreferencesSynchronize((CFStringRef)@"loginwindow", kCFPreferencesCurrentUser, kCFPreferencesAnyHost);
    
    [loginItems release];
    [pool release];    
}

JNIEXPORT jint JNICALL OS_NATIVE(SetDefaultFileTypeHandler)
    (JNIEnv *env, jobject this, jstring fileType, jstring applicationBundleIdentifier)
{
    OSErr theErr = -1;
    
    const char *fileTypeCstr = (*env)->GetStringUTFChars(env, fileType, NULL);
    if (fileTypeCstr == NULL) {
        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef fileTypeCFStr = CFStringCreateWithCString(NULL, fileTypeCstr,                                    
                                                  kCFStringEncodingMacRoman);

    CFStringRef utiForTorrents = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension,
                                                                       fileTypeCFStr,
                                                                       kUTTypeData);
    
    const char *applicationBundleIdentifierCstr = (*env)->GetStringUTFChars(env, applicationBundleIdentifier, NULL);
    if (applicationBundleIdentifierCstr == NULL) {
        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef applicationBundleIdentifierCFStr = CFStringCreateWithCString(NULL, applicationBundleIdentifierCstr,                                    
                                                                            kCFStringEncodingMacRoman);

    theErr = LSSetDefaultRoleHandlerForContentType(
                    utiForTorrents,
                    kLSRolesAll, 
                    applicationBundleIdentifierCFStr);
    
    (*env)->ReleaseStringUTFChars(env, fileType, fileTypeCstr);
    (*env)->ReleaseStringUTFChars(env, fileType, applicationBundleIdentifierCstr);

    CFRelease(fileTypeCFStr);
    CFRelease(applicationBundleIdentifierCFStr);
    CFRelease(utiForTorrents);

    return (jint)theErr;
}

JNIEXPORT jboolean JNICALL OS_NATIVE(IsApplicationTheDefaultFileTypeHandler)
(JNIEnv *env, jobject this, jstring fileType, jstring applicationBundleIdentifier)
{
    OSErr theErr = -1;
    
    const char *fileTypeCStr = (*env)->GetStringUTFChars(env, fileType, NULL);
    if (fileTypeCStr == NULL) {
        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef fileTypeCFStr = CFStringCreateWithCString(NULL, fileTypeCStr,                                    
                                                          kCFStringEncodingMacRoman);
    
    CFStringRef utiForTorrents = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension,
                                                                       fileTypeCFStr,
                                                                       kUTTypeData);

    CFStringRef defaultApplicationIdentifier = LSCopyDefaultRoleHandlerForContentType(utiForTorrents, kLSRolesAll);

    if ( defaultApplicationIdentifier == NULL )
        return false;
    
    const char *applicationBundleIdentifierCstr = (*env)->GetStringUTFChars(env, applicationBundleIdentifier, NULL);
    if (applicationBundleIdentifierCstr == NULL) {
        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef applicationBundleIdentifierCFStr = CFStringCreateWithCString(NULL, applicationBundleIdentifierCstr,                                    
                                                                            kCFStringEncodingMacRoman);

    bool isGivenApplicationTheDefaultFileTypeHandler = (CFStringCompare(defaultApplicationIdentifier, applicationBundleIdentifierCFStr, kCFCompareCaseInsensitive) == 0);

    (*env)->ReleaseStringUTFChars(env, fileType, fileTypeCStr);
    (*env)->ReleaseStringUTFChars(env, fileType, applicationBundleIdentifierCstr);

    CFRelease(fileTypeCFStr);
    CFRelease(utiForTorrents);
    CFRelease(defaultApplicationIdentifier);
    CFRelease(applicationBundleIdentifierCFStr);

    return isGivenApplicationTheDefaultFileTypeHandler;
}

JNIEXPORT jboolean JNICALL OS_NATIVE(IsFileTypeHandled)
(JNIEnv *env, jobject this, jstring fileType)
{
    OSErr theErr = -1;
    
    const char *fileTypeCStr = (*env)->GetStringUTFChars(env, fileType, NULL);
    if (fileTypeCStr == NULL) {
        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef fileTypeCFStr = CFStringCreateWithCString(NULL, fileTypeCStr,                                    
                                                          kCFStringEncodingMacRoman);
    
    //(*env)->GetStringUTFChars(env, fileType, true)
    CFStringRef utiForTorrents = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension,
                                                                       fileTypeCFStr,
                                                                       kUTTypeData);

    CFStringRef defaultApplicationIdentifier = LSCopyDefaultRoleHandlerForContentType(utiForTorrents, kLSRolesAll);

    bool isFileTypeHandled = (defaultApplicationIdentifier != NULL);

    (*env)->ReleaseStringUTFChars(env, fileType, fileTypeCStr);

    CFRelease(fileTypeCFStr);
    CFRelease(utiForTorrents);
    CFRelease(defaultApplicationIdentifier);

    return isFileTypeHandled;
}

/**
* This method returns all of the applications registered to handle the file type
* designated by the given file extension.
*/
JNIEXPORT jobjectArray JNICALL OS_NATIVE(GetAllHandlersForFileType)
(JNIEnv *env, jobject this, jstring fileType)
{
    const char *fileTypeCStr = (*env)->GetStringUTFChars(env, fileType, NULL);
    if (fileTypeCStr == NULL) {
        /* OutOfMemoryError already thrown */
        return NULL;
    }

    CFStringRef fileTypeCFStr = CFStringCreateWithCString(NULL, fileTypeCStr,                                    
                                                          kCFStringEncodingMacRoman);
    
    CFStringRef utiForTorrents = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension,
                                                                       fileTypeCFStr,
                                                                       kUTTypeData);

    CFArrayRef handlers = LSCopyAllRoleHandlersForContentType(utiForTorrents, kLSRolesAll);

    if (handlers == NULL) {
        CFRelease(fileTypeCFStr);
        CFRelease(utiForTorrents);
        
        return NULL;
    } else {
        // if we have a valid list of file URLs, then let's convert them to 
        // a java string array and pass it out.
        jclass strCls = (*env)->FindClass(env,"Ljava/lang/String;");
        jobjectArray handlerArray = (*env)->NewObjectArray(env, CFArrayGetCount(handlers), strCls, NULL);
    
        for (int counter = 0; counter < CFArrayGetCount(handlers); counter++) {
            CFStringRef applicationBundleIdentifier = CFArrayGetValueAtIndex(handlers, counter);
            
            CFRange range;
            range.location = 0;
            // Note that CFStringGetLength returns the number of UTF-16 characters,
            // which is not necessarily the number of printed/composed characters
            range.length = CFStringGetLength(applicationBundleIdentifier);
            UniChar charBuf[range.length];
            CFStringGetCharacters(applicationBundleIdentifier, range, charBuf);
            jstring applicationBundleIdentifierJavaStr = (*env)->NewString(env, (jchar *)charBuf, (jsize)range.length);

            // set the Java string in the java string array
            (*env)->SetObjectArrayElement(env, handlerArray, counter, applicationBundleIdentifierJavaStr);
            
            (*env)->DeleteLocalRef(env, applicationBundleIdentifierJavaStr);            
    
        }

        CFRelease(handlers);
        CFRelease(fileTypeCFStr);
        CFRelease(utiForTorrents);
        
        return handlerArray;
    }
}

#ifdef __cplusplus
}
#endif
