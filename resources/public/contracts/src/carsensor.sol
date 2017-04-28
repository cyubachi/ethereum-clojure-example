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
        bool isPaid;
        uint amount;
        uint limit;
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
        d.isPaid = false;
        d.amount = 0;
        dealers[dealerAddress] = d;
    }
    
    function addEnquiries (address from, address to, string message, uint registerDate) {
        Dealer dealer = dealers[to];
        Enquiry memory e;
        if (dealer.isPaid == false || (dealer.isPaid == true && dealer.limit < now)) {
            dealer = dealers[rAddress];
            e = Enquiry({from: from, to: to, encryptedMessage: message, registerDate: registerDate});
            dealer.enquiries.push(e);
            dealer.enquiryCount = dealer.enquiryCount + 1;
            dealers[rAddress] = dealer;
        } else {
            e = Enquiry({from: from, to: to, encryptedMessage: message, registerDate: registerDate});
            dealer.enquiries.push(e);
            dealer.enquiryCount = dealer.enquiryCount + 1;
            dealers[to] = dealer;
        }
    }
    
    function removeEnquiry(address addr, uint index) {
        Dealer dealer = dealers[addr];
        uint length = dealers[addr].enquiries.length-1;
        if (dealers[addr].enquiryCount > 0) {
            for (uint i = index ; i < length; i++) {
                dealers[addr].enquiries[i] = dealers[addr].enquiries[i+1];
            }
            delete dealers[addr].enquiries[length];
            dealers[addr].enquiries.length = length;
            dealers[addr].enquiryCount = dealer.enquiryCount - 1;
        }
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
        dealer.isPaid = true;
        // dealer.limit = now + (86400 * 30);
        dealer.limit = now + 300;
        sendPendingEnquiries(msg.sender);
        dealers[msg.sender] = dealer;
    }
    
    function getDealerEnquiry(address addr, uint index) constant returns (address, address, string, uint) {
        Dealer memory dealer = dealers[addr];
        return (dealer.enquiries[index].from, dealer.enquiries[index].to, dealer.enquiries[index].encryptedMessage, dealer.enquiries[index].registerDate);
    }
    
    function getDealer(address addr) constant returns (uint, string, address, bool, uint, uint) {
        return (dealers[addr].enquiryCount, dealers[addr].name, dealers[addr].ethAccount, dealers[addr].isPaid, dealers[addr].amount, dealers[addr].limit);
    }
    
    function kill() {
        suicide(msg.sender);
    }

}

