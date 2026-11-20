BUILD THE APK WITHOUT ANDROID STUDIO (GitHub Actions)
1. Create a free account at github.com and make a new repository.
2. Upload ALL files from inside this folder (not the folder itself) to the repo.
   The file .github/workflows/build.yml must be included. If it is missing
   (hidden folders are sometimes skipped), click "Add file > Create new file",
   type  .github/workflows/build.yml  as the name, and paste the contents of
   build-workflow.yml.txt.
3. Open the "Actions" tab > "Build APK" > wait ~5-8 minutes for the green tick.
4. Open the finished run, download "BanglaOCR-apk" (zip) from Artifacts,
   unzip, and install app-debug.apk on your phone (allow "install unknown apps").
