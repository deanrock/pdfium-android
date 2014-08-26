//
//  zoomobjects.h
//  samples
//
//  Created by Dean on 22/08/14.
//
//

#ifndef __samples__zoomobjects__
#define __samples__zoomobjects__

#include <iostream>
#include <vector>

class Rectangle {
public:
    int top;
    int bottom;
    int left;
    int right;
    std::vector<Rectangle*>rectangles;
    Rectangle *parent;
    
    void append(Rectangle *rect);
};

std::vector<Rectangle*> getRectangles(int width, int height, std::vector<Rectangle*>objects);

#endif /* defined(__samples__zoomobjects__) */
