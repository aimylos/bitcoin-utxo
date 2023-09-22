# blockchainj
Bitcoin UTXO analysis tool.

This project was created as part of an academic thesis in 2018. The link to the paper can be found [here](https://www.dropbox.com/scl/fi/vtz4h809vnh15tex0l9ko/Binding-Bitcoin-s-State-to-it-s-Blocks.pdf?rlkey=bdr9gspizu5l0a4a0pzg9bba5&dl=0).

The purpose of `blockchainj` project is to create and update the UTXO set for every block and calculate usefull information about the set.
> UTXO is the [Bitcoin's Unspent Transaction Output set](https://developer.bitcoin.org/devguide/transactions.html)

Both `Statistics` methods and a `Server` is included to measure performance, calculate stats and perform RPC calls to receive protocol-like data about the UTXO set.


# blockchainj/Bitcoin
Uses RPC calls to [`Bitcoin Core`](https://bitcoin.org/en/bitcoin-core/) to process the blocks into `Block` objects and creates a buffer to be used by the main application.


# blockchainj/Blockchain

## `Blockchain`
Maintains a `UtxoSet` object by processing `Block`s and updating the set accordingly.
<img title="Overall diagram" src="https://github.com/galeos4/blockchainj/assets/145703956/76ca1219-0e7d-40fa-bd99-d3430c2f0f0e" width="400">

## `UtxoSet`
Maintains a UTXO set following the schematic bellow. 
- Supports serialization and deserialization.
- Supports parameterization.
<img title="UtxoSet schema" src="https://github.com/galeos4/blockchainj/assets/145703956/e36799d3-bde6-4404-99e2-f88342adb489" width="500">

## `Server`
Given a `Blockchain`, runs a server that services RPC calls which provide data about the UTXO set using a Prototype Protocol.

## `Statistics`
Tracks the `UtxoSet` while `Blockchain` is building it and calculates useful information about the UTXO set. Evaluations can be found in the paper linked above.


# User Parameters
All parameters for running the system can be found and editted in the `src/Blockchainj/Blockchain/Main/UserParams.java` file.
