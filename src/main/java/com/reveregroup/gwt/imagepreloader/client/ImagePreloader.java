package com.reveregroup.gwt.imagepreloader.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * This library comes from http://code.google.com/p/gwt-image-loader/
 * We have patched it so it works in Internet Explorer more reliably.
 */
public class ImagePreloader {
    private static Map<String, Dimensions> dimensionCache = new HashMap<String, Dimensions>();
    private static final String INTERNET_EXPLORER = "msie";

    private static List<ImageLoader> activeLoaders = new ArrayList<ImageLoader>();

    private static Element loadingArea;

    public static Dimensions getCachedDimensions(String url) {
        return dimensionCache.get(url);
    }

    /**
     * Call this method to preload an image.
     *
     * @param url         - the image to pre-load
     * @param loadHandler - (optional) specify an ImageLoadHandler to be fired when the
     *                    image is fully loaded. Within this handler you will also be
     *                    able to get the original dimensions of the loaded image.
     */
    public static void load(String url, ImageLoadHandler loadHandler) {
        if (url == null) {
            if (loadHandler != null)
                loadHandler.imageLoaded(new ImageLoadEvent(url, null));
            return;
        }

        if (dimensionCache.containsKey(url)) {
            if (loadHandler != null) {
                Dimensions cachedDimensions = dimensionCache.get(url);
                if (cachedDimensions.getWidth() == -1)
                    // image load failed
                    loadHandler.imageLoaded(new ImageLoadEvent(url, null));
                    //FireLaterTimer.fireLater(loadHandler, new ImageLoadEvent(url, null));
                else
                    // image load succeeded
                    loadHandler.imageLoaded(new ImageLoadEvent(url, cachedDimensions));
                //FireLaterTimer.fireLater(loadHandler, new ImageLoadEvent(url, cachedDimensions));
            }
            return;
        } else {
            int index = findUrlInPool(url);
            if (index != -1) {
                activeLoaders.get(index).addHander(loadHandler);
                return;
            }
        }

        init();

        ImageLoader loader = new ImageLoader();
        activeLoaders.add(loader);
        loader.addHander(loadHandler);
        loader.start(url);
    }

    private static void init() {
        if (loadingArea == null) {
            loadingArea = DOM.createDiv();
            loadingArea.getStyle().setProperty("visibility", "hidden");
            loadingArea.getStyle().setProperty("position", "absolute");
            loadingArea.getStyle().setProperty("width", "1px");
            loadingArea.getStyle().setProperty("height", "1px");
            loadingArea.getStyle().setProperty("overflow", "hidden");
            Document.get().getBody().appendChild(loadingArea);
            Event.setEventListener(loadingArea, new EventListener() {
                public void onBrowserEvent(Event event) {
                    handleBrowserEvent(event);
                }
            });
        }
    }

    private static void handleBrowserEvent(Event event) {
        boolean success;
        if (Event.ONLOAD == event.getTypeInt()) {
            success = true;
        } else if (Event.ONERROR == event.getTypeInt()) {
            success = false;
        } else {
            return;
        }

        if (!ImageElement.is(event.getCurrentEventTarget())) {
            return;
        }

        ImageElement image = ImageElement.as(Element.as(event.getCurrentEventTarget()));
        prepareImage(success, image);
    }

    private static void prepareImage(boolean success, ImageElement image) {
        int index = findImageInPool(image);
        ImageLoader loader = activeLoaders.get(index);

        Dimensions dim = null;
        if (success) {
            dim = new Dimensions(image.getWidth(), image.getHeight());
            dimensionCache.put(loader.url, dim);
        } else {
            dimensionCache.put(loader.url, new Dimensions(-1, -1));
        }

        loadingArea.removeChild(image);
        activeLoaders.remove(index);

        ImageLoadEvent evt = new ImageLoadEvent(image, dim);
        loader.fireHandlers(evt);
    }

    private static int findImageInPool(ImageElement image) {
        for (int index = 0; index < activeLoaders.size(); index++) {
            if (activeLoaders.get(index).imageEquals(image)) {
                return index;
            }
        }
        return -1;
    }

    private static int findUrlInPool(String url) {
        for (int index = 0; index < activeLoaders.size(); index++) {
            if (activeLoaders.get(index).urlEquals(url)) {
                return index;
            }
        }
        return -1;
    }

    private static class ImageLoader {
        ImageElement image = DOM.createImg().cast();
        List<ImageLoadHandler> handlers;
        String url;

        public ImageLoader() {
            if (getUserAgent().contains(INTERNET_EXPLORER)) {
                simulateIEOnLoadEvent(); //onload event is not always fired reliably in IE -> do polling for 'complete' property instead.
            } else {
                Event.sinkEvents(image, Event.ONLOAD | Event.ONERROR);
            }
            loadingArea.appendChild(image);
        }

        private void simulateIEOnLoadEvent() {
            final int[] elapsedTime = {0};
            final int delayMs = 50;
            final int maxWaitingTime = 5000;
            Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
                public boolean execute() {
                    boolean imgComplete = image.getPropertyBoolean("complete");
                    if (imgComplete) {
                        prepareImage(true, image);
                        return false;
                    } else if (elapsedTime[0] > maxWaitingTime) {
                        prepareImage(false, image);
                        return false;
                    } else {
                        elapsedTime[0] += delayMs;
                        return true;
                    }
                }
            }, delayMs);
        }

        public static native String getUserAgent() /*-{
            return navigator.userAgent.toLowerCase();
        }-*/;

        public void clearHandlers() {
            if (handlers != null)
                handlers.clear();
        }

        public void addHander(ImageLoadHandler handler) {
            if (handler != null) {
                if (handlers == null) {
                    handlers = new ArrayList<ImageLoadHandler>(1);
                }
                handlers.add(handler);
            }
        }

        public void fireHandlers(ImageLoadEvent event) {
            if (handlers != null) {
                for (ImageLoadHandler handler : handlers) {
                    handler.imageLoaded(event);
                }
            }
        }

        public void start(String url) {
            this.url = url;
            image.setSrc(url);
        }

        public boolean imageEquals(ImageElement image) {
            return this.image == image;
        }

        public boolean urlEquals(String url) {
            return this.url.equals(url);
        }
    }

//	private static class FireLaterTimer extends Timer {
//		ImageLoadHandler handler;
//		ImageLoadEvent event;
//		
//		public FireLaterTimer(ImageLoadHandler handler, ImageLoadEvent event) {
//			this.handler = handler;
//			this.event = event;
//		}
//		
//		@Override
//		public void run() {
//			handler.imageLoaded(event);
//		}
//		
//		public static void fireLater(ImageLoadHandler handler, ImageLoadEvent event) {
//			new FireLaterTimer(handler, event).schedule(1);
//		}
//	}
}
