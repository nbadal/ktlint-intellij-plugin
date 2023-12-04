  #!/bin/bash
  if [ $(find plugin/build/idea-sandbox/plugins -name instrumented*.jar | wc -l | cut -w -f 2) -eq 1 ]; then
      (cd plugin/build/idea-sandbox/plugins && zip  -r ../../ktlint-plugin.zip ktlint)
      echo "Created ZIP file ./plugin/build/ktlint-plugin.zip"
  else
      echo "Can not create valid ZIP file. Run clean build first"
  fi
