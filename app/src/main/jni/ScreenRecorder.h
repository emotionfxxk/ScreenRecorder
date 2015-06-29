#ifndef _Included_ScreenRecorder
#define _Included_ScreenRecorder

class ScreenRecorder {
    public:
        static ScreenRecorder* getRecorder();
        bool init(int width, int height, int bitrate, int timeLimit, bool rotate, const char* destFilePath);
        bool start();
        bool stop();
    private:
        ScreenRecorder();
};

#endif
