/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * dhcpcd_test.cpp - unit tests for dhcpcd
 */

#include <stdint.h>
#include <stdlib.h>
#include <string>

#include <gtest/gtest.h>

// For convenience.
#define ARRAYSIZE(x) sizeof((x)) / sizeof((x)[0])

// Regrettably, copy these defines and the dhcp_message structure in from
// dhcp.h.  This header file is not easily included, since subsequent
// includes use C++ reserved keywords (like "new") as structure member names.
extern "C" {

#define DHO_PAD                 0
#define DHO_DNSDOMAIN           15

/* Max MTU - defines dhcp option length */
#define MTU_MAX             1500

/* Sizes for DHCP options */
#define DHCP_CHADDR_LEN         16
#define SERVERNAME_LEN          64
#define BOOTFILE_LEN            128
#define DHCP_UDP_LEN            (14 + 20 + 8)
#define DHCP_FIXED_LEN          (DHCP_UDP_LEN + 226)
#define DHCP_OPTION_LEN         (MTU_MAX - DHCP_FIXED_LEN)

/* Some crappy DHCP servers require the BOOTP minimum length */
#define BOOTP_MESSAGE_LENTH_MIN 300

struct dhcp_message {
        uint8_t op;           /* message type */
        uint8_t hwtype;       /* hardware address type */
        uint8_t hwlen;        /* hardware address length */
        uint8_t hwopcount;    /* should be zero in client message */
        uint32_t xid;            /* transaction id */
        uint16_t secs;           /* elapsed time in sec. from boot */
        uint16_t flags;
        uint32_t ciaddr;         /* (previously allocated) client IP */
        uint32_t yiaddr;         /* 'your' client IP address */
        uint32_t siaddr;         /* should be zero in client's messages */
        uint32_t giaddr;         /* should be zero in client's messages */
        uint8_t chaddr[DHCP_CHADDR_LEN];  /* client's hardware address */
        uint8_t servername[SERVERNAME_LEN];    /* server host name */
        uint8_t bootfile[BOOTFILE_LEN];    /* boot file name */
        uint32_t cookie;
        uint8_t options[DHCP_OPTION_LEN]; /* message options - cookie */
} _packed;

char * get_option_string(const struct dhcp_message *dhcp, uint8_t option);

}


static const char kOptionString[] = "hostname";

class DhcpcdGetOptionTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    memset(dhcpmsgs, 0, ARRAYSIZE(dhcpmsgs) * sizeof(struct dhcp_message));
    // Technically redundant.
    memset(&(dhcpmsgs[0].options), DHO_PAD, sizeof(dhcpmsgs[0].options));

    type_index = 0;
    length_index = 0;
    value_index = 0;
  }

  void PopulateTLV() {
    // May very well write off the end of the first struct dhcp_message,
    // by design.
    length_index = type_index + 1;
    value_index = length_index + 1;
    dhcpmsgs[0].options[type_index] = DHO_DNSDOMAIN;
    dhcpmsgs[0].options[length_index] = strlen(kOptionString);
    memcpy(&(dhcpmsgs[0].options[value_index]),
           kOptionString, strlen(kOptionString));
  }

  struct dhcp_message dhcpmsgs[2];
  volatile size_t type_index;
  volatile size_t length_index;
  volatile size_t value_index;
};

TEST_F(DhcpcdGetOptionTest, OptionNotPresent) {
  // An entire option block of padding (all zeros).
  EXPECT_EQ(NULL, ::get_option_string(&(dhcpmsgs[0]), DHO_DNSDOMAIN));
}

TEST_F(DhcpcdGetOptionTest, TypeIsOffTheEnd) {
  type_index = sizeof(dhcpmsgs[0].options);
  PopulateTLV();
  EXPECT_EQ(NULL, ::get_option_string(&(dhcpmsgs[0]), DHO_DNSDOMAIN));
}

TEST_F(DhcpcdGetOptionTest, LengthIsOffTheEnd) {
  type_index = sizeof(dhcpmsgs[0].options) - 1;
  PopulateTLV();
  EXPECT_EQ(NULL, ::get_option_string(&(dhcpmsgs[0]), DHO_DNSDOMAIN));
}

TEST_F(DhcpcdGetOptionTest, ValueIsOffTheEnd) {
  type_index = sizeof(dhcpmsgs[0].options) - 2;
  PopulateTLV();
  EXPECT_EQ(NULL, ::get_option_string(&(dhcpmsgs[0]), DHO_DNSDOMAIN));
}

TEST_F(DhcpcdGetOptionTest, InsufficientSpaceForValue) {
  type_index = sizeof(dhcpmsgs[0].options) - 6;
  PopulateTLV();
  char* value = ::get_option_string(&(dhcpmsgs[0]), DHO_DNSDOMAIN);
  EXPECT_TRUE(NULL != value);
  EXPECT_EQ("host", ::std::string(value));
  free(value);
}

TEST_F(DhcpcdGetOptionTest, InsufficientSpaceForContinuedValue) {
  type_index = sizeof(dhcpmsgs[0].options) - 16;
  PopulateTLV();
  type_index = sizeof(dhcpmsgs[0].options) - 6;
  PopulateTLV();
  char* value = ::get_option_string(&(dhcpmsgs[0]), DHO_DNSDOMAIN);
  EXPECT_TRUE(NULL != value);
  EXPECT_EQ("hostnamehost", ::std::string(value));
  free(value);
}
