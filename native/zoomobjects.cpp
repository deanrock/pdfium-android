//
//  zoomobjects.cpp
//  samples
//
//  Created by Dean on 22/08/14.
//
//

#include "zoomobjects.h"
#include <sstream>

void Rectangle::append(Rectangle *rect)
{
    if (this->left > rect->left) {
        this->left = rect->left;
    }
    
    if (this->top > rect->top) {
        this->top = rect->top;
    }
    
    if (this->right < rect->right) {
        this->right = rect->right;
    }
    
    if (this->bottom < rect->bottom) {
        this->bottom = rect->bottom;
    }
}

std::vector<Rectangle*> getRectangles(int width, int height, std::vector<Rectangle>objects)
{
    std::vector<Rectangle*>left;
    std::vector<Rectangle*>zoomRectangles;
    
    for (int i = 0; i < (int)objects.size(); i++) {
        left.push_back(&objects.at(i));
    }
    
    for (int i = 0; i < (int)objects.size(); i++) {
        Rectangle *rect = &objects.at(i);
        
        Rectangle *zoom;
        
        if (rect->parent == NULL) {
            zoom = new Rectangle();
            zoom->left = width;
            zoom->top = height;
            zoom->append(rect);
        }else{
            zoom = rect->parent;
        }
        
        int margin = 10;
        
        std::vector<Rectangle *>::iterator it = left.begin();
        
        while(it != left.end()) {
            Rectangle *rect2 = *it;
            
            if (
                //rect2->parent == NULL
                //&&
                ((rect->left > rect2->left-margin && rect->left < rect2->left+margin)
                 || (rect->right > rect2->right-margin && rect->right < rect2->right+margin))
                &&
                ((rect->bottom > rect2->top - margin && rect->bottom < rect2->top + margin)
                 || (rect->top > rect2->bottom + margin && rect->top < rect2->bottom + margin))
                ) {
                zoom->append(rect2);
                rect2->parent = zoom;
                //it = left.erase(it);
            }
            
            if (it != left.end()) {
                ++it;
            }
        }
        
        //if (!zoom->parent) {
        zoomRectangles.push_back(zoom);
        //}
    }
    
    /* JSON */
    /*std::stringstream out("");
    
    out << "{\"width\": "<<width<<", \"height\": "<<height<<", \"rects\": [";
    
    for (int i = 0; i < (int)zoomRectangles.size(); i++) {
        if (i>0) {
            out<<", ";
        }
        
        Rectangle *r = zoomRectangles.at(i);
        
        out << "{\"top\": "<<r->top
        <<", \"left\": "<<r->left
        <<", \"right\": "<<r->right
        <<", \"bottom\": "<<r->bottom
        <<"}";
    }
    
    out << "]}";
    
    return out.str();*/

    return zoomRectangles;
}
