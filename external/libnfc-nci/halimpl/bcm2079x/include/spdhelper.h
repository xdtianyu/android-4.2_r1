#pragma once

#include <string>

using namespace std;
#define DEFAULT_SPD_MAXRETRYCOUNT (3)

class SpdHelper
{
public:
    static bool isPatchBad(UINT8* prm, UINT32 len);
    static void setPatchAsBad();
    static void incErrorCount();
    static bool isSpdDebug();

private:
    SpdHelper();
    static SpdHelper& getInstance();

    bool isPatchBadImpl(UINT8* prm, UINT32 len);
    void setPatchAsBadImpl();
    void incErrorCountImpl();
    bool isSpdDebugImpl() {return mSpdDebug;}
    string mPatchId;
    int  mErrorCount;
    int  mMaxErrorCount;
    bool mIsPatchBad;
    bool mSpdDebug;
};
