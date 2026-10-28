(ns example.user
  (:require
    [schema.core :as s]))

(s/defschema User
  {:name s/Str
   :age s/Int})

(meta #'User)

(s/defn get-user :- User
  "doc"
  [id :- s/Uuid db]
  nil)

(meta #'get-user)

(s/defn multi :- (s/maybe s/Uuid)
  ([x :- s/Int] x)
  ([x :- s/Int [a b] :- [s/Int] & more :- [User]] x))

(meta #'multi)
