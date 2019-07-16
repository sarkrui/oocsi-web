name := """oocsi-web"""
organization := "IndustrialDesign"

version := "0.3.1"

maintainer := "m.funk@tue.nl"

scalaVersion := "2.12.8"

//offline := true

libraryDependencies ++= Seq(
  guice,
  javaWs
)

lazy val root = (project in file(".")).enablePlugins(PlayJava)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile)

// Java project. Don't expect Scala IDE
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java
// Use .class files instead of generated .scala files for views and routes 
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources)