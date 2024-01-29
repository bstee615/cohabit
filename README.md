# cohabit-backend

The backend for the Cohabit app.

## Usage

- Run server with logs: `bash serve.sh`
- Install dependencies: `lein deps`
- Run server: `lein run`
- Run tests: `lein test`

## Usage with nginx reverse proxy

Websockets require some extra configuration when hosted behind an nginx proxy. Add these lines to the `location` block inside the nginx configuration for your site:

```conf
location / {
  # other lines...
  proxy_set_header Upgrade $http_upgrade;
  proxy_set_header Connection "Upgrade";
}
```

## License

Copyright © 2024 Benjamin Steenhoek

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
