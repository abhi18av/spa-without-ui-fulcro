(ns app.youtube-scratch.youtube
  (:require [com.wsscode.pathom.connect :as pc]
            [app.youtube-scratch.youtube.activities :as yt.activities]
            [app.youtube-scratch.youtube.channels :as yt.channels]
            [app.youtube-scratch.youtube.playlists :as yt.playlists]
            [app.youtube-scratch.youtube.search :as yt.search]
            [app.youtube-scratch.youtube.videos :as yt.videos]))

(defn youtube-plugin []
  {::pc/register [yt.activities/resolvers
                  yt.channels/resolvers
                  yt.playlists/resolvers
                  yt.search/resolvers
                  yt.videos/resolvers]})

