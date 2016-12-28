pragma solidity ^0.4.6;

contract carsensor {
    struct Enquiry {
        address from;
        address to;
        string encryptedMessage;
        uint registerDate;
    }
    struct Dealer {
        Enquiry[] enquiries;
        uint enquiryCount;
        string name ;
        address ethAccount;
        bool isPayed;
        uint payedAmount;
    }
    Dealer templateDealer;
    
    mapping (address => Dealer) public dealers;
    
    address rAddress = 0x62ded09e27f2876ce3e4fee8e3ae9f9448508415;
    
    function carsensor() {
        putDealer(rAddress, "Recruit");
    }
    
    function putDealer(address dealerAddress, string name) {
        Dealer d = templateDealer;
        d.enquiryCount = 0;
        d.name = name;
        d.ethAccount = dealerAddress;
        d.isPayed = false;
        d.payedAmount = 0;
        dealers[dealerAddress] = d;
    }
    
    function addEnquiries (address from, address to, string message, uint registerDate) {
        Dealer dealer = dealers[to];
        Enquiry memory e;
        if (dealer.isPayed == false) {
            dealer = dealers[rAddress];
            e = Enquiry({from: from, to: to, encryptedMessage: message, registerDate: registerDate});
            dealer.enquiries.push(e);
            dealer.enquiryCount = dealer.enquiries.length;
            dealers[rAddress] = dealer;
        } else {
            e = Enquiry({from: from, to: to, encryptedMessage: message, registerDate: registerDate});
            dealer.enquiries.push(e);
            dealer.enquiryCount = dealer.enquiries.length;
            dealers[to] = dealer;
        }
    }
    
    function removeEnquiry(address addr, uint index) {
        Dealer dealer = dealers[addr];
        for (uint i = index ; i < dealer.enquiries.length-1; i++) {
            dealer.enquiries[i] = dealer.enquiries[i+1];
        }
        delete dealer.enquiries[dealer.enquiries.length-1];
        dealer.enquiryCount = dealer.enquiries.length;
        dealers[addr] = dealer;
    }
    
    function sendPendingEnquiries(address dealerAddress) {
        Dealer admin = dealers[rAddress];
        Dealer dealer = dealers[dealerAddress];
        
        for(uint i = 0; i < admin.enquiries.length; i++) {
            if (admin.enquiries[i].to == dealerAddress) {
                dealer.enquiries.push(admin.enquiries[i]);
                dealer.enquiryCount = dealer.enquiries.length;
                removeEnquiry(rAddress, i);
                i--;
            }
        }
        
        dealers[rAddress] = admin;
        dealers[dealerAddress] = dealer;
    }

    function () payable {
        Dealer dealer = dealers[msg.sender];
        if (msg.value < 10000000000000000) {
           throw;
        }
        dealer.isPayed = true;
        sendPendingEnquiries(msg.sender);
        dealers[msg.sender] = dealer;
    }
    
    function getDealerEnquiry(address addr, uint index) constant returns (address, address, string, uint) {
        return (dealers[addr].enquiries[index].from, dealers[addr].enquiries[index].to, dealers[addr].enquiries[index].encryptedMessage, dealers[addr].enquiries[index].registerDate);
    }
    
    function getDealer(address addr) constant returns (uint, string, address, bool, uint) {
        return (dealers[addr].enquiryCount, dealers[addr].name, dealers[addr].ethAccount, dealers[addr].isPayed, dealers[addr].payedAmount);
    }
    
    function kill() {
        suicide(msg.sender);
    }

}

