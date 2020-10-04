module alpha.medix.sleepy {
  requires javafx.controls;
  requires javafx.media;
  requires com.google.common;
  requires okhttp3;
  requires org.eclipse.jgit;
  requires jsch;
  requires com.fasterxml.jackson.databind;
  requires slf4j.jdk14;
  requires org.bouncycastle.provider;
  requires org.apache.commons.lang3;
  requires com.fasterxml.jackson.core;
  exports alpha.medix.sleepy;
  opens alpha.medix.sleepy.model to com.fasterxml.jackson.databind;
}