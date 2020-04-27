package com.jeffmony.async.http.server;

public interface RouteMatcher {
    AsyncHttpServerRouter.RouteMatch route(String method, String path);
}