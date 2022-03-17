# Admin Page for telegram-trash-talk bot
##Required tools
Application requires node with 16 version and last version of sbt.
(Application is incompatible with 17 version of node)


## Compiling JS
### For development:

**Enable recompile on save:**
```
sbt ~fastLinkJS
```


**Start Vite dev server:**
```
npm run dev
```

Then you can open application at http://localhost:3000 in browser.
The dev server supports hot reload.

### For testing
You need to install [Chrome Web driver](https://www.gregbrisebois.com/posts/chromedriver-in-wsl2/) to run tests.

Make sure the dev server is running:
```
npm run dev
```

Run tests:
```
sbt test
```

### For production:
1. **Generate fully-optimized JS**
```
sbt fullLinkJS
```
2. **Make sure `index.html` has the following line (uncomment it)**
```html
    <script type="module" src="/target/scala-2.13/trash-talk-bot-frontend-opt/main.js"></script>
```

3. **Bundle JS with dependencies**
```
npm run build
```

4. **(Optional) Serve generated bundle**:
```
npm run serve
```

Result will be generated at `dist` directory. It minifies.
