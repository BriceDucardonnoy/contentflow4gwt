/**
 * Copyright (c) 2011 Canoo Engineering AG info@canoo.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.gwt.contentflow4gwt.client;

import java.util.ArrayList;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

public class ContentFlow<T> extends Composite {
    private final ArrayList<ContentFlowItemClickListener> fContentFlowItemClickListeners;
    private final ArrayList<ContentFlowItemScrollListener> fContentFlowItemScrollListeners;
    private final ArrayList<Widget> fContentFlowItems;
    private final ContentFlowWrapper fContentFlowWrapper;
    private final ContentFlowTemplate fContentFlowTemplate;

    public ContentFlow() {
        this(false, false);
    }

    public ContentFlow(boolean showGlobalCaption, boolean showScrollbar) {
        this(showGlobalCaption, showScrollbar, 0.5f, 0.8f, 0.5f, false, "none");
    }

    /**
     * @param showGlobalCaption Should the caption text below the content be shown?
     * @param showScrollbar Should the scrollbar be shown?
     * @param reflectionHeight How tall the reflection should be: 0.5f = 50%
     * @param flowSpeedFactor
     * @param flowDragFriction
     * @param circularFlow True if you want the content to wrap around (ie, never end)
     * @param scrollInFrom  One of 'next', 'right', 'pre', 'previous', 'left' or 'none'.
     */
    public ContentFlow(boolean showGlobalCaption, boolean showScrollbar, float reflectionHeight, final float flowSpeedFactor, final float flowDragFriction, final boolean circularFlow, final String scrollInFrom) {
        fContentFlowItemClickListeners = new ArrayList<ContentFlowItemClickListener>();
        fContentFlowItemScrollListeners = new ArrayList<ContentFlowItemScrollListener>();
        fContentFlowItems = new ArrayList<Widget>();
        fContentFlowTemplate = new ContentFlowTemplate(showGlobalCaption, showScrollbar);
        fContentFlowWrapper = createContentFlow(fContentFlowTemplate.getId(), reflectionHeight, flowSpeedFactor, flowDragFriction, circularFlow, scrollInFrom);
        initWidget(fContentFlowTemplate.asWidget());
    }

    private native ContentFlowWrapper createContentFlow(String id, float _reflectionHeight, float _flowSpeedFactor, float _flowDragFriction, boolean _circularFlow, String _scrollInFrom) /*-{
        // ContentFlow extends the Function prototype with the method bind. Do the same in the actual "window" context
        Function.prototype.bind = $wnd.Function.prototype.bind;

        // Save in a reference to the GWT contentflow4gwt object to allow calling back in the right instance
        var gwtContentFlow = this;
        var conf = {
            onclickActiveItem : function(item) {
                // Do the callback in GWT using the saved reference because "this" will be bound when invoked
                gwtContentFlow.@org.gwt.contentflow4gwt.client.ContentFlow::itemClick(I)(item.index);
            },
            onReachTarget : function(item) {
              gwtContentFlow.@org.gwt.contentflow4gwt.client.ContentFlow::onReachTarget(I)(item.index);
            },
            startItem : 'start',
            scrollInFrom : _scrollInFrom,
            flowSpeedFactor : _flowSpeedFactor,
            reflectionHeight: _reflectionHeight,
            flowDragFriction: _flowDragFriction,
            scrollWheelSpeed: 1.0,
            circularFlow: _circularFlow
        };

        var log = $wnd.log; //may be used to debug content flow script

        // Create an object of the type ContentFlow in the right "window" context
        return new $wnd.ContentFlow(id, conf);
    }-*/;

    @Override
    protected void onLoad() {
        super.onLoad();
        // Initialize the object after it has been added to the DOM, if not ContentFlow cannot find DOM elements
        fContentFlowWrapper.init();
    }

    private void onReachTarget(int itemIndex) {
        onReachTarget(getItem(itemIndex));
    }

    private void onReachTarget(Widget item) {
        for (ContentFlowItemScrollListener listener : fContentFlowItemScrollListeners) {
            listener.onReachTarget(item);
        }
    }

    private void itemClick(int itemIndex) {
        itemClick(getItem(itemIndex));
    }

    public void itemClick(Widget item) {
        for (ContentFlowItemClickListener listener : fContentFlowItemClickListeners) {
            listener.onItemClicked(item);
        }
    }

    public void addItems(Widget... items) {
        for (Widget item : items) {
            fContentFlowTemplate.addWidget(item);
            // DO NOT USE fContentFlowWrapper.addItem()! items are already added using addWidget()!
            fContentFlowItems.add(item);
        }
    }
    
    public void addItem(Widget item, int index) {
    	Log.info("add item");
    	fContentFlowTemplate.addWidget(item, index);
    	fContentFlowWrapper.addItem(item.getElement(), index);
    	// FIXME BDY: works but don't keep order and may need to move current position to re-have good names
    	Log.info("add done");
        // DO NOT USE fContentFlowWrapper.addItem()! items are already added using addWidget()!
//        fContentFlowItems.add(item);
    }
    
    public int getNumberOfItems() {
    	return fContentFlowItems.size();
    }
    /*
     * Test
     */
    /**
     * Removes items from DOM but keep it in memory
     * @param items to delete
     */
    public void removeItems(Widget... items) {
    	int nb = fContentFlowItems.size();
    	int ind;
    	for (Widget item : items) {
//    		fContentFlowItems.remove(item);
    		for(ind = 0 ; ind < nb ; ind++) {
    			if(fContentFlowItems.get(ind).equals(item)) break;
    		}
//    		Log.info("remove item index: " + ind);
    		if(ind ==nb) {
    			Log.warn("Widget not found => doesn't remove it");
    			return;
    		}
    		fContentFlowWrapper.removeItem(ind);
        }
    }
    
    public void remove() {
    	fContentFlowItems.remove(3);
    	fContentFlowWrapper.removeItem(3);
    	// Fire a scroll event
    	// Remove scroll / click listener
//    	moveTo(getItem(1));// Is it usefull?
    }
    /*
     * 
     */
    public Widget getItem(int itemIndex) {
//    	if(itemIndex < fContentFlowItems.size()) return fContentFlowItems.get(itemIndex);
//    	return null;
    	return fContentFlowItems.get(itemIndex);
    }

    public Widget getActiveItem() {
        ContentFlowItemWrapper activeItem = fContentFlowWrapper.getActiveItem();
        if (null != activeItem) {
            return fContentFlowItems.get(activeItem.getIndex());
        } else {
            return null;
        }
    }

    public void moveTo(Widget item) {
        fContentFlowWrapper.moveTo(fContentFlowItems.indexOf(item));
    }

    public HandlerRegistration addItemClickListener(final ContentFlowItemClickListener clickListener) {
        fContentFlowItemClickListeners.add(clickListener);
        return new HandlerRegistration() {
            public void removeHandler() {
                fContentFlowItemClickListeners.remove(clickListener);
            }
        };
    }

    public HandlerRegistration addItemScrollListener(final ContentFlowItemScrollListener clickListener) {
        fContentFlowItemScrollListeners.add(clickListener);
        return new HandlerRegistration() {
            public void removeHandler() {
                fContentFlowItemScrollListeners.remove(clickListener);
            }
        };
    }

    private static class ContentFlowTemplate implements IsWidget {
        private static final String CONTENT_FLOW_CLASS = "ContentFlow";
        private static final String FLOW_TEMPLATE = "<div class=\"flow\"></div>";
        private static final String GLOBAL_CAPTION_TEMPLATE = "<div class=\"globalCaption\"></div>";
        private static final String SCROLLBAR_TEMPLATE = "<div class=\"scrollbar\"><div class=\"slider\"></div></div>";

        private static int nextId = 1;

        private final HTMLPanel fWidget;
        private final Node fItemContainer;

        public ContentFlowTemplate(boolean showGlobalCaption, boolean showScrollbar) {
            this(showGlobalCaption, showScrollbar, "_contentFlow_" + nextId++);
        }

        public ContentFlowTemplate(boolean showGlobalCaption, boolean showScrollbar, String id) {
            String html = FLOW_TEMPLATE;
            if (showGlobalCaption) {
                html += GLOBAL_CAPTION_TEMPLATE;
            } if (showScrollbar) {
                html += SCROLLBAR_TEMPLATE;
            }
            fWidget = new HTMLPanel(html);
            fWidget.getElement().setId(id);
            fWidget.getElement().setClassName(CONTENT_FLOW_CLASS);
            fItemContainer = fWidget.getElement().getChild(0);
        }

        public String getId() {
            return fWidget.getElement().getId();
        }

        public void addWidget(Widget widget) {
            fWidget.add(widget, fItemContainer.<Element>cast());
        }
        
        public void addWidget(Widget widget, int index) {
        	if(index > fWidget.getElement().getChildCount()) {
        		index = fWidget.getElement().getChildCount();
        	}
        	if(index == 0) {
        		// FIXME BDY: cause an exception
//        		fItemContainer.insertBefore(widget.getElement(), fWidget.getWidget(index).getElement());
        	}
        	else {
        		fItemContainer.insertAfter(widget.getElement(), fWidget.getWidget(index - 1).getElement());
        	}
        }
        
//        public void removeWidget(Widget widget) {
////        	fItemContainer.getFirstChild().removeFromParent();
////        	fWidget.remove(widget);
//        	fWidget.getWidget(getWidgetIndex(widget)).removeFromParent();
//        }
//        
//        public int getWidgetIndex(Widget widget) {
//        	return fWidget.getWidgetIndex(widget);
//        }

        public Widget asWidget() {
            return fWidget;
        }
    }
}
