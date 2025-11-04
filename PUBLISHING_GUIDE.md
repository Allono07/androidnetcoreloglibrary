# Publishing Netcore SDK Capturer to Maven Central

This guide will walk you through publishing your obfuscated SDK to Maven Central.

## ✅ What's Already Set Up

1. ✅ ProGuard obfuscation configured
2. ✅ Maven publishing plugin added
3. ✅ POM metadata configured
4. ✅ Sources and Javadoc JARs enabled
5. ✅ Signing configuration added

## 📋 Prerequisites

### 1. Create Sonatype Account

1. Go to [Sonatype JIRA](https://issues.sonatype.org/secure/Signup!default.jspa)
2. Create an account
3. Create a new issue to claim your group ID:
   - Project: Community Support - Open Source Project Repository Hosting (OSSRH)
   - Issue Type: New Project
   - Group Id: `com.android.netcoresdkcapturer`
   - Project URL: `https://github.com/Allono07/assignmentAppJava`
   - SCM URL: `https://github.com/Allono07/assignmentAppJava.git`

4. Wait for approval (usually 1-2 business days)

### 2. Generate GPG Key

```bash
# Generate a new GPG key
gpg --gen-key

# List your keys to get the key ID
gpg --list-secret-keys --keyid-format=long

# The output will look like:
# sec   rsa3072/YOUR_KEY_ID 2024-11-04
# Use the YOUR_KEY_ID part

# Export the key to a file
gpg --export-secret-keys -o ~/.gnupg/secring.gpg

# Publish your public key
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### 3. Configure Credentials

Create/update `~/.gradle/gradle.properties`:

```properties
# Sonatype credentials
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password

# GPG signing
signing.keyId=YOUR_KEY_ID
signing.password=your-gpg-passphrase
signing.secretKeyRingFile=/Users/yourusername/.gnupg/secring.gpg
```

**⚠️ IMPORTANT**: Never commit this file to Git!

## 🚀 Publishing Steps

### Step 1: Update Version

In `NetcoreSDKCapturer/build.gradle.kts`, update the version:

```kotlin
val sdkVersion = "1.0.0"  // Change this for each release
```

### Step 2: Build the AAR

```bash
cd /Users/allen.thomson/AndroidStudioProjects/assignmentAppJava

# Clean build
./gradlew :NetcoreSDKCapturer:clean

# Build release AAR (obfuscated)
./gradlew :NetcoreSDKCapturer:assembleRelease
```

The obfuscated AAR will be at:
`NetcoreSDKCapturer/build/outputs/aar/NetcoreSDKCapturer-release.aar`

### Step 3: Verify Obfuscation

```bash
# Extract and check the AAR
unzip NetcoreSDKCapturer/build/outputs/aar/NetcoreSDKCapturer-release.aar -d temp/
# Check classes - they should be obfuscated (a.class, b.class, etc.)
```

### Step 4: Publish to Maven Central

```bash
# Publish to Sonatype staging repository
./gradlew :NetcoreSDKCapturer:publishReleasePublicationToSonatypeRepository
```

### Step 5: Close and Release on Sonatype

1. Go to [Sonatype Nexus](https://s01.oss.sonatype.org)
2. Login with your credentials
3. Click "Staging Repositories" in left menu
4. Find your repository (usually at bottom)
5. Select it and click "Close" button
6. Wait for validation (5-10 minutes)
7. If validation passes, click "Release" button
8. Your artifact will be available on Maven Central in ~2 hours

## 📦 Publishing to GitHub Packages (Alternative)

If you want to publish to GitHub Packages instead:

### Update build.gradle.kts:

```kotlin
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Allono07/assignmentAppJava")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### Publish:

```bash
./gradlew :NetcoreSDKCapturer:publish
```

## 🔍 Testing the Published SDK

In a test project's `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.android.netcoresdkcapturer:netcore-sdk-capturer:1.0.0")
}
```

## 📝 Version Management

### Semantic Versioning

- **1.0.0** - Initial release
- **1.0.1** - Patch (bug fixes)
- **1.1.0** - Minor (new features, backward compatible)
- **2.0.0** - Major (breaking changes)

### Snapshot Versions

For testing, use SNAPSHOT versions:

```kotlin
val sdkVersion = "1.0.1-SNAPSHOT"
```

Snapshots can be published repeatedly without releasing.

## 🛠️ Useful Commands

```bash
# Check what will be published
./gradlew :NetcoreSDKCapturer:publishToMavenLocal

# Check in ~/.m2/repository/com/android/netcoresdkcapturer/

# Generate Javadoc
./gradlew :NetcoreSDKCapturer:dokkaHtml

# List all tasks
./gradlew :NetcoreSDKCapturer:tasks
```

## 📋 Checklist Before Publishing

- [ ] Updated version number
- [ ] Updated README.md
- [ ] Updated CHANGELOG.md (create if needed)
- [ ] Tested locally with `publishToMavenLocal`
- [ ] ProGuard rules verified
- [ ] All tests passing
- [ ] Documentation complete
- [ ] License file present
- [ ] Git tag created: `git tag v1.0.0`

## 🔐 Security Best Practices

1. Never commit credentials to Git
2. Use environment variables for CI/CD
3. Rotate GPG keys annually
4. Enable 2FA on Sonatype account
5. Review ProGuard mapping files (keep them private)

## 📞 Support

If you encounter issues:
- Sonatype Guide: https://central.sonatype.org/publish/
- GitHub Discussions: Create an issue in your repo
- Stack Overflow: Tag with `maven-publish`, `android-library`

## 🎉 Success!

Once published, users can add your SDK with:

```kotlin
dependencies {
    implementation("com.android.netcoresdkcapturer:netcore-sdk-capturer:1.0.0")
}
```

The SDK is obfuscated but the public API (`SdkLogCaptor` and `LogPayloadCallback`) remains accessible!
