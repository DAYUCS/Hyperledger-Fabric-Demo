/*
Copyright 2016 Chinasystems
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
Licensed Materials - Property of Chinasystems
© Copyright Chinasystems Corp. 2016
*/
package main

import (
	"encoding/json"
	"fmt"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// TradeFinanceChaincode example simple Chaincode implementation
type TradeFinanceChaincode struct {
}

// TradeFinance Data
type TradeFinance struct {
	Reference           string `json:"reference"`
	Exporter            string `json:"exporter"`
	ShippingCorporation string `json:"shippingCorporation"`
	NegotiationBank     string `json:"negotiationBank"`
	ImportBank          string `json:"importBank"`
	Status              string `json:"status"`
}

// Init function
func (t *TradeFinanceChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
	_, args := stub.GetFunctionAndParameters()

	if len(args) != 0 {
		return shim.Error("Incorrect number of arguments. Expecting 0")
	}

	return shim.Success(nil)
}

// Invoke runs callback representing the invocation of a chaincode
func (t *TradeFinanceChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	function, args := stub.GetFunctionAndParameters()

	if function != "invoke" {
		return shim.Error("Unknown function call")
	}

	if len(args) < 2 {
		return shim.Error("Incorrect number of arguments. Expecting at least 2")
	}

	if args[0] == "query" {
		return t.query(stub, args)
	}

	if args[0] == "delete" {
		// Deletes an entity from its state
		return t.delete(stub, args)
	}

	if args[0] == "inspect" {
		// Inspection
		return t.inspect(stub, args)
	}

	if args[0] == "ship" {
		// Shipping (B/L)
		return t.ship(stub, args)
	}

	if args[0] == "present" {
		// Present Documents
		return t.present(stub, args)
  }

	if args[0] == "arrival" {
		// Documents Arrival
		return t.arrival(stub, args)
  }

	if args[0] == "payment" {
		// Payment/Settlement
		return t.payment(stub, args)
  }

	return shim.Error("Unknown action, check the first argument, must be one of 'delete', 'query', 'inspect', 'ship', 'present', 'arrival' or 'payment'")

}

// Inspection
func (t *TradeFinanceChaincode) inspect(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	fmt.Println("Inspecting Goods...")

	if len(args) != 4 {
		return shim.Error("Incorrect number of arguments. Expecting 4, function followed by reference number, exporter and shipping corporation")
	}

	var tradeFinance TradeFinance
	var err error

	tradeFinance.Reference = args[1]
	tradeFinance.Exporter = args[2]
	tradeFinance.ShippingCorporation = args[3]
	tradeFinance.NegotiationBank = ""
	tradeFinance.ImportBank = ""
	tradeFinance.Status = "Inspection"

	tradeFinanceBytes, err := json.Marshal(&tradeFinance)
	if err != nil {
		fmt.Println("Error marshalling tradeFinance to json")
		return shim.Error("Error Marshalling Inspection Data")
	}
	err = stub.PutState(tradeFinance.Reference, tradeFinanceBytes)
	if err != nil {
		fmt.Println("Error saving inspection data")
		return shim.Error("Error saving inspection data")
	}

	if transientMap, err := stub.GetTransient(); err == nil {
		if transientData, ok := transientMap["result"]; ok {
			return shim.Success(transientData)
		}
	}
	return shim.Success(nil)
}

// Ship goods (B/L)
func (t *TradeFinanceChaincode) ship(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	fmt.Println("Bill of Lading...")

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2, function followed by reference number")
	}

	var tradeFinance TradeFinance
	var tradeFinanceOld TradeFinance
	var err error

	tradeFinance.Reference = args[1]

	fmt.Println("Getting State on reference number " + tradeFinance.Reference)
	tradeFinanceBytes, err := stub.GetState(tradeFinance.Reference)
	if tradeFinanceBytes == nil {
		return shim.Error("Failed retrieving transaction on reference number " + tradeFinance.Reference)
	}

	err = json.Unmarshal(tradeFinanceBytes, &tradeFinanceOld)
	if err != nil {
		fmt.Println("Error Unmarshalling tradeFinanceBytes")
		return shim.Error("Error unmarshalling transaction on reference number " + tradeFinance.Reference)
	}

	if tradeFinanceOld.Status != "Inspection" {
		fmt.Println("Error status of transaction")
		return shim.Error("Error status of transaction " + tradeFinanceOld.Status)
	}

	tradeFinance.Exporter = tradeFinanceOld.Exporter
	tradeFinance.ShippingCorporation = tradeFinanceOld.ShippingCorporation
	tradeFinance.NegotiationBank = tradeFinanceOld.NegotiationBank
	tradeFinance.ImportBank = tradeFinanceOld.ImportBank
	tradeFinance.Status = "B/L"

	tradeFinanceBytesNew, err := json.Marshal(&tradeFinance)
	if err != nil {
		fmt.Println("Error marshalling tradeFinance")
		return shim.Error("Error marshalling tradeFinance")
	}

	err = stub.PutState(tradeFinance.Reference, tradeFinanceBytesNew)
	if err != nil {
		fmt.Println("Error saving B/L status")
		return shim.Error("Error saving B/L status")
	}

	if transientMap, err := stub.GetTransient(); err == nil {
		if transientData, ok := transientMap["result"]; ok {
			return shim.Success(transientData)
		}
	}
	return shim.Success(tradeFinanceBytesNew)
}

// Present Documents
func (t *TradeFinanceChaincode) present(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	fmt.Println("Present Documents...")

	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting 3, function followed by reference number and negotiation bank")
	}

	var tradeFinance TradeFinance
	var tradeFinanceOld TradeFinance
	var err error

	tradeFinance.Reference = args[1]
	tradeFinance.NegotiationBank = args[2]

	fmt.Println("Getting State on reference number " + tradeFinance.Reference)
	tradeFinanceBytes, err := stub.GetState(tradeFinance.Reference)
	if tradeFinanceBytes == nil {
		return shim.Error("Failed retrieving transaction on reference number " + tradeFinance.Reference)
	}

	err = json.Unmarshal(tradeFinanceBytes, &tradeFinanceOld)
	if err != nil {
		fmt.Println("Error Unmarshalling tradeFinanceBytes")
		return shim.Error("Error unmarshalling transaction on reference number " + tradeFinance.Reference)
	}

	if tradeFinanceOld.Status != "B/L" {
		fmt.Println("Error status of transaction")
		return shim.Error("Error status of transaction " + tradeFinanceOld.Status)
	}

	tradeFinance.Exporter = tradeFinanceOld.Exporter
	tradeFinance.ShippingCorporation = tradeFinanceOld.ShippingCorporation
	tradeFinance.ImportBank = tradeFinanceOld.ImportBank
	tradeFinance.Status = "Present Documents"

	tradeFinanceBytesNew, err := json.Marshal(&tradeFinance)
	if err != nil {
		fmt.Println("Error marshalling tradeFinance")
		return shim.Error("Error marshalling tradeFinance")
	}

	err = stub.PutState(tradeFinance.Reference, tradeFinanceBytesNew)
	if err != nil {
		fmt.Println("Error saving Present Documents status")
		return shim.Error("Error saving Present Documents status")
	}

	if transientMap, err := stub.GetTransient(); err == nil {
		if transientData, ok := transientMap["result"]; ok {
			return shim.Success(transientData)
		}
	}
	return shim.Success(tradeFinanceBytesNew)
}

// Documents Arrival
func (t *TradeFinanceChaincode) arrival(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	fmt.Println("Documents Arrival...")

	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting 3, function followed by reference number and import bank")
	}

	var tradeFinance TradeFinance
	var tradeFinanceOld TradeFinance
	var err error

	tradeFinance.Reference = args[1]
	tradeFinance.ImportBank = args[2]

	fmt.Println("Getting State on reference number " + tradeFinance.Reference)
	tradeFinanceBytes, err := stub.GetState(tradeFinance.Reference)
	if tradeFinanceBytes == nil {
		return shim.Error("Failed retrieving transaction on reference number " + tradeFinance.Reference)
	}

	err = json.Unmarshal(tradeFinanceBytes, &tradeFinanceOld)
	if err != nil {
		fmt.Println("Error Unmarshalling tradeFinanceBytes")
		return shim.Error("Error unmarshalling transaction on reference number " + tradeFinance.Reference)
	}

	if tradeFinanceOld.Status != "Present Documents" {
		fmt.Println("Error status of transaction")
		return shim.Error("Error status of transaction " + tradeFinanceOld.Status)
	}

	tradeFinance.Exporter = tradeFinanceOld.Exporter
	tradeFinance.ShippingCorporation = tradeFinanceOld.ShippingCorporation
	tradeFinance.NegotiationBank = tradeFinanceOld.NegotiationBank
	tradeFinance.Status = "Documents Arrival"

	tradeFinanceBytesNew, err := json.Marshal(&tradeFinance)
	if err != nil {
		fmt.Println("Error marshalling tradeFinance")
		return shim.Error("Error marshalling tradeFinance")
	}

	err = stub.PutState(tradeFinance.Reference, tradeFinanceBytesNew)
	if err != nil {
		fmt.Println("Error saving Documents Arrival status")
		return shim.Error("Error saving Documents Arrival status")
	}

	if transientMap, err := stub.GetTransient(); err == nil {
		if transientData, ok := transientMap["result"]; ok {
			return shim.Success(transientData)
		}
	}
	return shim.Success(tradeFinanceBytesNew)
}

// Payment
func (t *TradeFinanceChaincode) payment(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	fmt.Println("Payment...")

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2, function followed by reference number")
	}

	var tradeFinance TradeFinance
	var tradeFinanceOld TradeFinance
	var err error

	tradeFinance.Reference = args[1]

	fmt.Println("Getting State on reference number " + tradeFinance.Reference)
	tradeFinanceBytes, err := stub.GetState(tradeFinance.Reference)
	if tradeFinanceBytes == nil {
		return shim.Error("Failed retrieving transaction on reference number " + tradeFinance.Reference)
	}

	err = json.Unmarshal(tradeFinanceBytes, &tradeFinanceOld)
	if err != nil {
		fmt.Println("Error Unmarshalling tradeFinanceBytes")
		return shim.Error("Error unmarshalling transaction on reference number " + tradeFinance.Reference)
	}

	if tradeFinanceOld.Status != "Documents Arrival" {
		fmt.Println("Error status of transaction")
		return shim.Error("Error status of transaction " + tradeFinanceOld.Status)
	}

	tradeFinance.Exporter = tradeFinanceOld.Exporter
	tradeFinance.ShippingCorporation = tradeFinanceOld.ShippingCorporation
	tradeFinance.NegotiationBank = tradeFinanceOld.NegotiationBank
	tradeFinance.ImportBank = tradeFinanceOld.ImportBank
	tradeFinance.Status = "Payment"

	tradeFinanceBytesNew, err := json.Marshal(&tradeFinance)
	if err != nil {
		fmt.Println("Error marshalling tradeFinance")
		return shim.Error("Error marshalling tradeFinance")
	}

	err = stub.PutState(tradeFinance.Reference, tradeFinanceBytesNew)
	if err != nil {
		fmt.Println("Error saving Payment status")
		return shim.Error("Error saving Payment status")
	}

	if transientMap, err := stub.GetTransient(); err == nil {
		if transientData, ok := transientMap["result"]; ok {
			return shim.Success(transientData)
		}
	}
	return shim.Success(tradeFinanceBytesNew)
}

// Deletes an entity from state
func (t *TradeFinanceChaincode) delete(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2, function followed by reference number")
	}

	Ref := args[1]

	// Delete the key from the state in ledger
	err := stub.DelState(Ref)
	if err != nil {
		return shim.Error("Failed to delete state")
	}

	return shim.Success(nil)
}

// Query callback representing the query of a chaincode
func (t *TradeFinanceChaincode) query(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2, function followed by reference number")
	}

	// ref number
	Ref := args[1]

	fmt.Println("Getting State on reference number " + Ref)
	TradeFinanceBytes, err := stub.GetState(Ref)
	if err != nil {
		return shim.Error("Failed to query state")
	}
	if TradeFinanceBytes == nil {
		return shim.Error("Failed retrieving transaction on reference number " + Ref)
	}
	//fmt.Println("Result: " + string(TradeFinanceBytes))

	return shim.Success(TradeFinanceBytes)
}

func main() {
	err := shim.Start(new(TradeFinanceChaincode))
	if err != nil {
		fmt.Printf("Error starting Trade Finance chaincode: %s", err)
	}
}
