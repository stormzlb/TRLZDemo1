//
// Created by Admin on 2019/3/22.
//

#ifndef TRLZDEMO1_LIBZTMATCH_H
#define TRLZDEMO1_LIBZTMATCH_H

#include <cstdint>
#include <stddef.h>
#define LONG int
#define BOOL bool

#define TRUE true
#define FALSE false

typedef struct
{
    LONG x;
    LONG y;
} ZTMATCH_POINT;

typedef struct _ZTMATCH_PTLIST_NODE
{
    ZTMATCH_POINT pt;
    struct _ZTMATCH_PTLIST_NODE* pNext;
} ZTMATCH_PTLIST_NODE;

typedef struct
{
    ZTMATCH_PTLIST_NODE *pPointList;
} ZTMATCH_STROKE;

typedef struct
{
    ZTMATCH_POINT ptTurnCenter;
    float fTurnA;
    float fScale;
    LONG nOffsetX;
    LONG nOffsetY;
} ZTMATCH_MRESULT;

typedef enum
{
    STROKE_ANALYZE_NONE = 0,
    STROKE_ANALYZE_DEVIATION,				// 偏移
    STROKE_ANALYZE_SHORTER,				// 太短
    STROKE_ANALYZE_LONGER					// 太长
} ZTMATCH_STROKE_ANALYZE;

typedef struct
{
    int nScore;
    ZTMATCH_STROKE_ANALYZE enumAnalyze;
    int nAnalyzeArg;
} ZTMATCH_EVALUATE;

void ZTMATCH_FreeStroke(ZTMATCH_STROKE* pStroke);
BOOL ZTMATCH_GetZiTieStroke(char *pZTData, int nZTDataLen, int *pResultXSize, int *pResultYSize, ZTMATCH_STROKE*** pppResultStroke, int *pResultStrokeCount);
BOOL ZTMATCH_Match(char *pZTData, int nZTDataLen, ZTMATCH_STROKE** ppUserStroke, int nUserStrokeCount, ZTMATCH_MRESULT *pMatchResult, ZTMATCH_EVALUATE *pEvaluate);

int Test(int nTest);

#endif //TRLZDEMO1_LIBZTMATCH_H
