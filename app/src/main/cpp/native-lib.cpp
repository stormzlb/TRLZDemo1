#include <jni.h>
#include <string>
#include "libZTMatch.h"

void AddTail(ZTMATCH_PTLIST_NODE* pList, ZTMATCH_PTLIST_NODE* pNode)
{
    if (NULL == pList)
        return;

    while (pList->pNext)
    {
        pList = pList->pNext;
    }

    pList->pNext = pNode;
    pNode->pNext = NULL;
}


extern "C" JNIEXPORT jstring JNICALL
Java_cn_tureal_trlzdemo1_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";

    int ret = Test(3);
    hello = std::to_string(ret);
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT int JNICALL
Java_cn_tureal_trlzdemo1_MainActivity_Test2(
        JNIEnv *env,
        jobject  jobj/* this */, jbyteArray ZiTieStream, jintArray UserStrokeStream, jintArray StrokeScore) {

    jbyte *pZiTieData = NULL;
    ZTMATCH_STROKE ** ppUserStroke = NULL;
    int nUserStrokeCount = 0;
    ZTMATCH_MRESULT mResult;
    ZTMATCH_EVALUATE evaluate[50];

    BOOL bOK = TRUE;

    do {
        jbyte *arrayZiTie = env->GetByteArrayElements(ZiTieStream, JNI_FALSE);
        jint *arrayUserStroke = env->GetIntArrayElements(UserStrokeStream, JNI_FALSE);

        // Prepare ZiTie Data
        jsize szArrayZiTie = env->GetArrayLength(ZiTieStream);
        jsize szArrayUserStroke = env->GetArrayLength(UserStrokeStream);

        int nZTDataLen = (int) szArrayZiTie;
        if (0 == nZTDataLen) {
            bOK = FALSE;
            break;
        }

        pZiTieData = new jbyte[nZTDataLen];
        if (NULL == pZiTieData) {
            bOK = FALSE;
            break;
        }

        memcpy(pZiTieData, arrayZiTie, nZTDataLen);

        nUserStrokeCount = arrayUserStroke[0];
        if (0 == nUserStrokeCount) {
            bOK = FALSE;
            break;
        }

        ppUserStroke = new ZTMATCH_STROKE*[nUserStrokeCount];
        if (NULL == ppUserStroke) {
            bOK = FALSE;
            break;
        }

        for (int i = 0; i < nUserStrokeCount; i++)
            ppUserStroke[i] = NULL;

        int nCurrentIdx = 1;
        int nCurrentStrokeCount = 0;
        while (nCurrentStrokeCount < nUserStrokeCount) {
            ppUserStroke[nCurrentStrokeCount] = new ZTMATCH_STROKE;
            if (NULL == ppUserStroke[nCurrentStrokeCount]) {
                bOK = FALSE;
                break;
            }

            ppUserStroke[nCurrentStrokeCount]->pPointList = NULL;

            int nPointCount = arrayUserStroke[nCurrentIdx++];

            for (int i = 0; i < nPointCount; i++) {
                ZTMATCH_PTLIST_NODE *pNode = new ZTMATCH_PTLIST_NODE;
                if (pNode)
                {
                    pNode->pt.x = arrayUserStroke[nCurrentIdx++];
                    pNode->pt.y = arrayUserStroke[nCurrentIdx++];
                    pNode->pNext = NULL;

                    if (ppUserStroke[nCurrentStrokeCount]->pPointList == NULL)
                    {
                        ppUserStroke[nCurrentStrokeCount]->pPointList = pNode;
                    }
                    else
                    {
                        AddTail(ppUserStroke[nCurrentStrokeCount]->pPointList, pNode);
                    }
                }
            }

            nCurrentStrokeCount++;
        }

        if (bOK)
            bOK = ZTMATCH_Match((char*) pZiTieData, nZTDataLen, ppUserStroke, nUserStrokeCount, &mResult, evaluate);

    } while (0);

    if (pZiTieData) {
        delete []pZiTieData;
        pZiTieData = NULL;
    }

    if (ppUserStroke) {
        for (int i = 0; i < nUserStrokeCount; i++) {
            if (ppUserStroke[i]) {
                ZTMATCH_FreeStroke(ppUserStroke[i]);
                ppUserStroke[i] = NULL;
            }
        }

        delete []ppUserStroke;
        ppUserStroke = NULL;
    }

    if (bOK) {
        // write back result
        jclass cls=env->GetObjectClass(jobj);

        // turnCenterX
        jfieldID fieldId=env->GetFieldID(cls, "m_nTurnCenterX", "I");
        env->SetIntField(jobj, fieldId, mResult.ptTurnCenter.x);

        // turnCenterY
        fieldId=env->GetFieldID(cls, "m_nTurnCenterY", "I");
        env->SetIntField(jobj, fieldId, mResult.ptTurnCenter.y);

        // OffsetX
        fieldId=env->GetFieldID(cls, "m_nOffsetX", "I");
        env->SetIntField(jobj, fieldId, mResult.nOffsetX);

        // OffsetY
        fieldId=env->GetFieldID(cls, "m_nOffsetY", "I");
        env->SetIntField(jobj, fieldId, mResult.nOffsetY);

        // Scale
        fieldId=env->GetFieldID(cls, "m_fScale", "F");
        env->SetFloatField(jobj, fieldId, mResult.fScale);

        // turnAngle
        fieldId=env->GetFieldID(cls, "m_fTurnAngle", "F");
        env->SetFloatField(jobj, fieldId, mResult.fTurnA);

        // Score
        jint *arr = env->GetIntArrayElements(StrokeScore, NULL);
        for (int i = 0; i < nUserStrokeCount + 1; i++)
            arr[i] = evaluate[i].nScore;

        env->ReleaseIntArrayElements(StrokeScore, arr, JNI_COMMIT);
    }

    //std::string hello = "Hello from C++";
    //return env->NewStringUTF(hello.c_str());

    return bOK;
}
