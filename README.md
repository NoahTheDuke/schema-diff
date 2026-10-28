# com.noahbogart/schema-diff

clone the repo and pull the `other` branch too, then run `bb compare main other`. should output something like:

```
$ bb compare main other
diff --git a/output/main/example/user b/output/other/example/user
index e409bf2..b33952f 100644
--- a/output/main/example/user
+++ b/output/other/example/user
@@ -7,4 +7,4 @@ get-user
 (=> {:name Str, :age Int} Uuid Any)

 multi
-(=>* (maybe Int) (Int) (Int [Int] & [{:name Str, :age Int}]))
+(=>* (maybe Uuid) (Int) (Int [Int] & [{:name Str, :age Int}]))
```

## License

Copyright © 2026 Noah

Distributed under the Mozilla Public License version 2.0.
