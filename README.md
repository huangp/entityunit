Entity Unit
===========

Simple tool to make JPA entities for testing.

To use it:

```xml
<dependency>
  <groupId>com.github.huangp</groupId>
  <artifactId>entityunit</artifactId>
  <version>${entityunit.version}</version>
  <scope>test</scope>
</dependency>
```

See https://github.com/huangp/entityunit/wiki for more information.

### release

Refer to http://central.sonatype.org/pages/apache-maven.html

```bash
mvn versions:set -DnewVersion=1.2.3
git tag entityunit-1.2.3
git push --tags
# check settings.xml for sonatype credentials and gpg passphrase
mvn clean deploy

```

