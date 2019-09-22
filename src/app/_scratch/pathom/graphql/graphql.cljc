(ns app.-scratch.pathom.graphql.graphql)


(def graphql-config
  {::pcg/url       "https://api.graph.cool/simple/v1/cjo58bqvp7k070194uq8ff9g9"
   ::pcg/prefix    "conj-pathom"
   ::pcg/ident-map {}
   ::p.http/driver http-driver})

(comment
  (go
    (reset! gql-indexes (<! (pcg/load-index graphql-config)))
    (swap! indexes pc/merge-indexes @gql-indexes)))

