# google-genai ships no consumer rules; its request and response types are (de)serialized by Jackson through reflection.
-keep class com.google.genai.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
