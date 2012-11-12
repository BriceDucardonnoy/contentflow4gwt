package com.reveregroup.gwt.imagepreloader.client;

import com.google.gwt.event.shared.EventHandler;

public interface FitImageLoadHandler extends EventHandler {
	public void imageLoaded(FitImageLoadEvent event);
}