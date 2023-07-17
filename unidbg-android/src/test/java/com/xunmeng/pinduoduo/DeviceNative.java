package com.xunmeng.pinduoduo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

/**
 * 1.权限 读取本机识别码 要给权限 否则无法使用
 * 在拨号界面输入*#06#就会有个码(设置--关于手机 也可以查看) 就像人的身份证一样
 * IME1/MEID HEX/MEID DEC/序列号
 * 每个手机唯一 可以用来追踪定位(公安部门)
 */
public class DeviceNative extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final DvmClass cDeviceNative;
    private static final String APK_PATH = "/root/Downloads/study/Android/apk/pdd5.10.0.apk";
    private static final String SO_PATH = "/root/Downloads/study/Android/apk/pdd5.10.0/lib/armeabi-v7a/libPddSecure.so";
    private static final String PACKAGENAME = "com.xunmeng.pinduoduo";
    private static final String CLASSNAME = "com/xunmeng/pinduoduo/secure/DeviceNative";

    /**
     * 已授权
     */
    private static final int PERMISSION_GRANTED = 0;

    /**
     * 未授权
     */
    private static final int PERMISSION_DENIED = 1;

    /**
     * ANDROID_ID是设备首次启动时由系统随机生成的一串64位的十六进制数字 基本可以保证唯一性
     * 但是root、刷机或恢复出厂设置都会导致设备的ANDROID_ID重置 所以用它作为设备的唯一标识不太保险
     * import android.provider.Settings;
     * String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
     * System.out.println("androidId " + androidId);
     * // 随机生成
     * String genAndroidId = Long.toHexString(new SecureRandom().nextLong());
     * System.out.println("genAndroidId " + genAndroidId);
     */
    private static final String ANDROID_ID = "521a53e3f579aaf1";  // Pixel3 TODO

    /**
     * <uses-permission android:name="android.permission.READ_PRIVILEGED_PHONE_STATE"
     * tools:ignore="ProtectedPermissions" />
     * <uses-permission android:name="READ_PHONE_STATE" />
     * TelephonyManager tm =(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
     * String deviceId = tm.getDeviceId();
     */
    private static final String DEVICEID = "990012006431222/12";  // TODO IMEI

    private static final String SIMSERIALNUMBER = "89860112851083069510";  // TODO

    private static final int PHONE_TYPE_GSM = 1;

    /**
     * <a href="https://developer.android.com/reference/android/telephony/TelephonyManager?hl=en#SIM_STATE_UNKNOWN">...</a>
     * SIM card state: Ready
     * 1: no SIM card  5: Ready
     */
    private static final int SIM_STATE_READY = 5;

    /**
     * Current network is LTE
     * 13: LTE --> 4G
     */
    private static final int NETWORK_TYPE_LTE = 13;

    /**
     * Data connection state: Connected. IP traffic should be available.
     * 2: 指示系统基于运营商信令或运营商特权应用程序启用或禁用运营商数据。运营商数据的打开/关闭不会影响用户设置，但如果设置为false，则会绕过设置并在内部关闭数据。
     */
    private static final int DATA_CONNECTED = 2;

    /**
     * Data connection activity: No traffic.
     */
    private static final int DATA_ACTIVITY_NONE = 0;

    /**
     * Data connection activity: Currently sending IP PPP traffic.
     */
    private static final int DATA_ACTIVITY_OUT = 2;

    /**
     * Data connection activity: Currently both sending and receiving IP PPP traffic.
     */
    private static final int DATA_ACTIVITY_INOUT = 3;

    /**
     * Data connection is active, but physical link is down
     */
    private static final int DATA_ACTIVITY_DORMANT = 4;

    public DeviceNative() {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName(PACKAGENAME)
//                .addBackendFactory(new Unicorn2Factory(true))
                .build();

        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM((File) null);
//        vm = emulator.createDalvikVM(new File(APK_PATH));
        vm.setVerbose(false);
        vm.setJni(this);
        // vm.setDvmClassFactory(new ProxyClassFactory());  // bug

        // 加载多个so 依赖
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/armeabi-v7a/libPddSecure.so"), true);

        dm.callJNI_OnLoad(emulator);
        module = dm.getModule();
        cDeviceNative = vm.resolveClass(CLASSNAME);
    }

    public void destroy() throws IOException {
        emulator.close();
    }

    public String info2() {
        String methodSign = "info2(Landroid/content/Context;J)Ljava/lang/String;";
        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null);
        long j = 1688018564388L;
        StringObject obj = cDeviceNative.callStaticJniMethodObject(emulator, methodSign, context, j);
        return obj.getValue();
    }

    public static void main(String[] args) throws Exception {
        DeviceNative deviceNativeTest = new DeviceNative();
        String anti_token = deviceNativeTest.info2();
        System.out.println("anti_token=" + anti_token);
        deviceNativeTest.destroy();
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        if ("android/telephony/TelephonyManager->PHONE_TYPE_GSM:I".equals(signature)) {
            return PHONE_TYPE_GSM;
        }

        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        System.out.println("call " + signature);
        switch (signature) {
//            case "android/os/Build->SERIAL:Ljava/lang/String;":
//                return new StringObject(vm, "948X1YV56");  // 未访问
            case "android/provider/Settings$Secure->ANDROID_ID:Ljava/lang/String;":
                return new StringObject(vm, ANDROID_ID);
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public void callVoidMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/util/zip/GZIPOutputStream->write([B)V":
                OutputStream outputStream = (OutputStream) dvmObject.getValue();
                ByteArray array = varArg.getObjectArg(0);
                // Inspector.inspect(array.getValue(), "java/util/zip/GZIPOutputStream->write outputStream=" + outputStream.getClass().getName());
                try {
                    outputStream.write(array.getValue());
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            case "java/util/zip/GZIPOutputStream->finish()V":
                GZIPOutputStream gzipOutputStream = (GZIPOutputStream) dvmObject.getValue();
                try {
                    gzipOutputStream.finish();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            case "java/util/zip/GZIPOutputStream->close()V":
                gzipOutputStream = (GZIPOutputStream) dvmObject.getValue();
                try {
                    gzipOutputStream.close();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
        }

        super.callVoidMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "com/tencent/mars/xlog/PLog->i(Ljava/lang/String;Ljava/lang/String;)V":
                return;
        }
        super.callStaticVoidMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "android/os/Debug->isDebuggerConnected()Z":
                return false;
        }
        return super.callStaticBooleanMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "android/provider/Settings$Secure->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;":
                StringObject key = varArg.getObjectArg(1);
                if (ANDROID_ID.equals(key.getValue())) {
                    return new StringObject(vm, ANDROID_ID);
                } else {
                    System.out.println("android/provider/Settings$Secure->getString key=" + key.getValue());
                }
        }

        return callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "android/content/Context->checkSelfPermission(Ljava/lang/String;)I":
                String p = (String) varArg.getObjectArg(0).getValue();
                switch (p) {
                    case "android.permission.READ_PHONE_STATE":
                        return PERMISSION_GRANTED;
                    default:
                        System.out.println("========== checkSelfPermission " + p);
                        return PERMISSION_DENIED;
                }
            case "android/telephony/TelephonyManager->getSimState()I":
                return SIM_STATE_READY;
            case "android/telephony/TelephonyManager->getNetworkType()I":
                return NETWORK_TYPE_LTE;
            case "android/telephony/TelephonyManager->getDataState()I":
                return DATA_CONNECTED;
            case "android/telephony/TelephonyManager->getDataActivity()I":
                Random r = new Random();
                int index = r.nextInt(3);
                if (index == 0) {
                    return DATA_ACTIVITY_OUT;
                } else if (index == 1) {
                    return DATA_ACTIVITY_INOUT;
                } else {
                    return DATA_ACTIVITY_DORMANT;
                }
            case "android/telephony/TelephonyManager->getPhoneType()I":
                return PHONE_TYPE_GSM;
        }

        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "android/content/Context->getSystemService(Ljava/lang/String;)Ljava/lang/Object;":
                DvmClass clazz = vm.resolveClass("android/telephony/TelephonyManager");
                return clazz.newObject(null);
            case "android/telephony/TelephonyManager->getDeviceId()Ljava/lang/String;":
                return new StringObject(vm, DEVICEID);
            case "android/telephony/TelephonyManager->getDeviceId(I)Ljava/lang/String;":
                return new StringObject(vm, DEVICEID);
            case "android/telephony/TelephonyManager->getSimSerialNumber()Ljava/lang/String;":
                return new StringObject(vm, SIMSERIALNUMBER);
            case "android/telephony/TelephonyManager->getSimOperatorName()Ljava/lang/String;":
                return new StringObject(vm, "CMCC");  // 中国移动通信
            case "android/telephony/TelephonyManager->getSimCountryIso()Ljava/lang/String;":
                return new StringObject(vm, "cn");
            case "android/telephony/TelephonyManager->getSubscriberId()Ljava/lang/String;":
                return new StringObject(vm, "460013061600183");  // TODO
            case "android/telephony/TelephonyManager->getNetworkOperator()Ljava/lang/String;":
                // 中国移动：46000 46002 中国联通：46001 中国电信：46003
                return new StringObject(vm, "46000");
            case "android/telephony/TelephonyManager->getNetworkOperatorName()Ljava/lang/String;":
                return new StringObject(vm, "CMCC");
            case "android/telephony/TelephonyManager->getNetworkCountryIso()Ljava/lang/String;":
                return new StringObject(vm, "cn");
            case "android/content/Context->getContentResolver()Landroid/content/ContentResolver;":
                clazz = vm.resolveClass("android/content/ContentResolver");
                return clazz.newObject(null);
            case "java/lang/Throwable->getStackTrace()[Ljava/lang/StackTraceElement;":
                StackTraceElement[] elements = Thread.currentThread().getStackTrace();
                DvmObject<?>[] objs = new DvmObject[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    objs[i] = vm.resolveClass("java/lang/StackTraceElement").newObject(elements[i]);
                }
                return new ArrayObject(objs);
            case "java/lang/StackTraceElement->getClassName()Ljava/lang/String;":
                StackTraceElement element = (StackTraceElement) dvmObject.getValue();
                return new StringObject(vm, element.getClassName());

            case "java/io/ByteArrayOutputStream->toByteArray()[B":
                ByteArrayOutputStream baos = (ByteArrayOutputStream) dvmObject.getValue();
                byte[] data = baos.toByteArray();
                // Inspector.inspect(data, "java/io/ByteArrayOutputStream->toByteArray()");
                return new ByteArray(vm, data);
        }

        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
//            case "java/util/UUID->toString()Ljava/lang/String;":
//                UUID uuid = (UUID) dvmObject.getValue();
//                return new StringObject(vm, uuid.toString());
            case "java/lang/String->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;":
                StringObject str = (StringObject) dvmObject;
                StringObject s1 = vaList.getObjectArg(0);
                StringObject s2 = vaList.getObjectArg(1);
                return new StringObject(vm, str.getValue().replaceAll(s1.getValue(), s2.getValue()));
        }

        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "java/lang/Throwable-><init>()V":
                return dvmClass.newObject(null);
            case "java/io/ByteArrayOutputStream-><init>()V":
                return dvmClass.newObject(new ByteArrayOutputStream());
            case "java/util/zip/GZIPOutputStream-><init>(Ljava/io/OutputStream;)V":
                DvmObject<?> obj = varArg.getObjectArg(0);
                OutputStream outputStream = (OutputStream) obj.getValue();
                try {
                    return dvmClass.newObject(new GZIPOutputStream(outputStream));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
        }

        return super.newObject(vm, dvmClass, signature, varArg);
    }
}
