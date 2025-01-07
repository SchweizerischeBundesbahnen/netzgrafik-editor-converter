# Changelog

## [1.1.0](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/compare/v1.0.1...v1.1.0) (2024-12-13)


### Features

* configure manual workflow dispatch for release
  please ([#34](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/issues/34)) ([2ba16ab](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/2ba16ab81cbbefb3fa1d568ee45a92ce83cf0506))

## [1.0.1](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/compare/v1.0.0...v1.0.1) (2024-12-10)


### Bug Fixes

* **ci:** fix maven publish by providing
  settings.xml ([c8d4965](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/c8d496587be7367bc4677fe6bb63c2e366d1aaec))

## 1.0.0 (2024-12-10)

### Features

* add command line option to use a rolling stock CSV
  repo ([5607044](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/5607044343a6cffa36889d6438c529cf0f376df2))
* add command line
  runner ([b2e6f84](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/b2e6f845eed8b6a6eeb90bd00727f88ac36a54ad))
* add config to command line
  app ([55500be](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/55500bec42baad1d863b3d7e14bbb953bb4f78a0))
* add csv-based infrastructure
  repository ([ddd3d35](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/ddd3d35515bd45a82165873f39ba054a226f0d20))
* add network graphic
  validation ([0f9741c](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/0f9741c0c2a05655b87e61b0730d31e2a0414621))
* add picocli-based command line
  interface ([c849e25](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/c849e25f70a071d0d75679343531a3e2a017bf83))
* add required model components of GTFS
  static ([f1fc5b1](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/f1fc5b1a1c2f69a08f606118d7f55fead5cada61))
* add rolling stock CSV repository
  implementation ([69eb81a](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/69eb81a3693b583d184949012bc42a5dcfde4519))
* add validation strategy
  REMOVE_DOTS_AND_REPLACE_WHITESPACE ([1a3d988](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/1a3d988c4f2d949a8e9326f8400a88f695fc0269))
* allow user to choose how transit line id is
  constructed ([f5cbe9a](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/f5cbe9ad5608daeb9744e8638643874f658407f4))
* create directed sequences already in sequence
  builder ([770d65a](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/770d65adce09077940396ebef699821344fbcd54))
* default supply
  repositories ([fb71667](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/fb716678f7515221d750624fc921c388ad1038f7))
* enhance validation log
  messages ([1e1e197](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/1e1e197d717b7ba1e8c7ceeab859d01bf5b5f358))
* extend validation strategies for network graphic
  ids ([1715348](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/1715348cdc49371f4577e2a1b352bc9dd189dd21))
* extract generic base supply
  builder ([5934df6](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/5934df69dd0433668dd9b4b5e5b703484332f80d))
* follow matsim convention for offsets on first and last route
  stops ([cbb4227](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/cbb42270f7a226d597fe8c7922a95ff5f8ee71d4))
* initial version of netzgrafik
  converter ([84a0587](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/84a058740d0b0b97751f1861d4a55adb4b3183f4))
* initial version of trainrun
  iterator ([ba39dd1](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/ba39dd1ea68aaca9b3df70f5ee1d4a320abd6e24))
* introduce converter
  config ([b223b93](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/b223b930866662f905c6d710862238f4c4619dc3))
* introduce generic
  converter ([61c3be5](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/61c3be565c966dfceb5a5c58c72e0a86957c6c9e))
* introduce service day time value
  object ([524ae6d](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/524ae6df1108b8b2434d5f7ec2648047d7800ade))
* introduce source and sink
  interface ([0195b60](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/0195b60a5c35a80ffc0cb503d051fff165e383d6))
* move direction logic from supply builder to
  converter ([211b303](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/211b30340879efea4ca5ae56fc161118ca24a012))
* move loops to generic base supply
  builder ([9507d86](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/9507d86fb40f97d3d806e4deccd22bdae7e7c1f2))
* network graphic validator and
  sanitizer ([df7ccef](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/df7ccefbd222bb38c89b258ed8046b8c0d311754))
* option to use names of NGE train runs as transit line
  ids ([b666b16](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/b666b163dadc3aadb52a241e22b3185db9f287da))
* optionally write gtfs to
  zip ([9a841e0](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/9a841e0495242019e536c271ddd383d3becc278f))
* pass screen coordinates to infrastructure
  repo ([6059f13](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/6059f133b7bc7444d11c28468b646d0988a84016))
* remove deprecated constructor of
  BOMInputStream ([0e0047d](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/0e0047d76245bd23db38bbe17db998a81dff289c))
* remove TODO for
  validation ([a656c4b](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/a656c4b17d40ea070516f6b2abd0278f411c74a4))
* remove transitions from lookup since they are not
  needed ([8fe5fe9](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/8fe5fe9c7e576f84a31df4396b6f11f412ade9a2))
* schedule writer for
  gtfs ([89fb746](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/89fb746fb25101619fe616ee9f9f8beffbda645c))
* trainrun section iterator using ports and
  transitions ([9471fdb](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/9471fdbab78b87a9082eddd9ec1d916bb7370075))
* use service day time in converter instead of java local
  time ([6958d90](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/6958d9071d74601a7a87b540e58e585f334142ac))
* use stop name of stop
  facility ([bc5039c](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/bc5039c033a357315fc9fdf5c8555259d2da8d91))

### Bug Fixes

* add one gtfs trip per
  departure ([c64628f](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/c64628f3876ecefbbed4b4b074d706f8905d4066))
* add stop link when on a route
  pass ([9a5360d](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/9a5360d223c2c7585c225588a6c98dfc96e1a172))
* add stop links to transit
  routes ([4b545d4](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/4b545d4361d60deeb9f10d2f7566b88dd433bc9f))
* allow network routes that have only one
  link ([714ec0d](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/714ec0d5f78bfe71fb6a82718b893348f675c797))
* avoid sequence alignment if sequence consists only of one
  section ([5983314](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/5983314b728edc8710099ffbcc23973c976213db))
* correct departure and arrival time offsets in MATSim
  builder ([6e985ce](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/6e985ceab2d9a4d2eb92d078c6f8c8d1ca03fb76))
* correct gtfs schedule output
  format ([3f64653](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/3f6465392d2abebb870d0dd61ac8b68b61a63409))
* correct reverse route
  direction ([6d47bbd](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/6d47bbd8509ca0525c701464718a649ee2a47b50))
* correct stop
  times ([a6f0786](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/a6f0786a5263a33f30855262a11dffabc4fb3fe0))
* correct swap condition of first
  section ([842dbf5](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/842dbf5077c31e2ec32f6ca0f0fc63c4454fed0a))
* escape values for
  CSV ([fd01154](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/fd01154ea4f8898ba2b3e590869f47bab89217d8))
* overwrite valid field ranges from temporal to fit service day
  time ([b50fc44](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/b50fc44364abd1a0fa9e12f76cc91cf4de7059ad))
* reverse
  ordering ([170ac87](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/170ac87d125e61b7f4e4c52439a4996068bb33dd))
* set network mode on vehicle
  type ([8d4ab0a](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/8d4ab0a23d29dd50f8a6d962292cef43279d3e5a))
* set vehicle type
  info ([63fb613](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/63fb613df3e7bc9a6955c4b7df1bcbaa537b03da))
* swap
  coordinates ([da7c1bf](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter/commit/da7c1bf3506a9af1a8ada5776ea1a3f2a57f3597))
