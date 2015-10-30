#!/usr/bin/env python

# Copyright (c) 2015 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.
import contextlib
import unittest

import agents.atrace_agent as atrace_agent
import systrace
import util

DEVICE_SERIAL = 'AG8404EC0444AGC'
LIST_TMP_ARGS = ['ls', '/data/local/tmp']
ATRACE_ARGS = ['atrace', '-z', '-t', '10']
CATEGORIES = ['sched', 'gfx', 'view', 'wm']
ADB_SHELL = ['adb', '-s', DEVICE_SERIAL, 'shell']

SYSTRACE_CMD = ['./systrace.py', '--time', '10', '-o', 'out.html', '-e',
                DEVICE_SERIAL] + CATEGORIES
TRACE_CMD = (ADB_SHELL + ATRACE_ARGS + CATEGORIES +
             [';', 'ps', '-t'])

SYSTRACE_LIST_CATEGORIES_CMD = ['./systrace.py', '-e', DEVICE_SERIAL, '-l']
TRACE_LIST_CATEGORIES_CMD = (ADB_SHELL + ['atrace', '--list_categories'])

LEGACY_ATRACE_ARGS = ['atrace', '-z', '-t', '10', '-s']
LEGACY_TRACE_CMD = (ADB_SHELL + LEGACY_ATRACE_ARGS +
             [';', 'ps', '-t'])

TEST_DIR = 'test_data/'
ATRACE_DATA = TEST_DIR + 'atrace_data'
ATRACE_DATA_RAW = TEST_DIR + 'atrace_data_raw'
ATRACE_DATA_STRIPPED = TEST_DIR + 'atrace_data_stripped'
ATRACE_DATA_THREAD_FIXED = TEST_DIR + 'atrace_data_thread_fixed'
ATRACE_DATA_WITH_THREAD_LIST = TEST_DIR + 'atrace_data_with_thread_list'
ATRACE_THREAD_NAMES = TEST_DIR + 'atrace_thread_names'


class UtilUnitTest(unittest.TestCase):
  def test_construct_adb_shell_command(self):
    command = util.construct_adb_shell_command(LIST_TMP_ARGS, None)
    self.assertEqual(' '.join(command), 'adb shell ls /data/local/tmp')

    command = util.construct_adb_shell_command(LIST_TMP_ARGS, DEVICE_SERIAL)
    self.assertEqual(' '.join(command),
                     'adb -s AG8404EC0444AGC shell ls /data/local/tmp')

    command = util.construct_adb_shell_command(ATRACE_ARGS, DEVICE_SERIAL)
    self.assertEqual(' '.join(command),
                     'adb -s AG8404EC0444AGC shell atrace -z -t 10')


class AtraceAgentUnitTest(unittest.TestCase):
  def test_construct_trace_command(self):
    options, categories = systrace.parse_options(SYSTRACE_CMD)
    agent = atrace_agent.AtraceAgent(options, categories)
    tracer_args = agent._construct_trace_command()
    self.assertEqual(' '.join(TRACE_CMD), ' '.join(tracer_args))
    self.assertEqual(True, agent.expect_trace())

  def test_extract_thread_list(self):
    with contextlib.nested(open(ATRACE_DATA_WITH_THREAD_LIST, 'r'),
                           open(ATRACE_DATA_RAW, 'r'),
                           open(ATRACE_THREAD_NAMES, 'r')) as (f1, f2, f3):
      atrace_data_with_thread_list = f1.read()
      atrace_data_raw = f2.read()
      atrace_thread_names = f3.read()

      trace_data, thread_names = atrace_agent.extract_thread_list(
          atrace_data_with_thread_list)
      self.assertEqual(atrace_data_raw, trace_data)
      self.assertEqual(atrace_thread_names, str(thread_names))

  def test_strip_and_decompress_trace(self):
    with contextlib.nested(open(ATRACE_DATA_RAW, 'r'),
                           open(ATRACE_DATA_STRIPPED, 'r')) as (f1, f2):
      atrace_data_raw = f1.read()
      atrace_data_stripped = f2.read()

      trace_data = atrace_agent.strip_and_decompress_trace(atrace_data_raw)
      self.assertEqual(atrace_data_stripped, trace_data)

  def test_fix_thread_names(self):
    with contextlib.nested(
        open(ATRACE_DATA_STRIPPED, 'r'),
        open(ATRACE_THREAD_NAMES, 'r'),
        open(ATRACE_DATA_THREAD_FIXED, 'r')) as (f1, f2, f3):
      atrace_data_stripped = f1.read()
      atrace_thread_names = f2.read()
      atrace_data_thread_fixed = f3.read()
      thread_names = eval(atrace_thread_names)

      trace_data = atrace_agent.fix_thread_names(
          atrace_data_stripped, thread_names)
      self.assertEqual(atrace_data_thread_fixed, trace_data)

  def test_preprocess_trace_data(self):
    with contextlib.nested(open(ATRACE_DATA_WITH_THREAD_LIST, 'r'),
                           open(ATRACE_DATA, 'r')) as (f1, f2):
      atrace_data_with_thread_list = f1.read()
      atrace_data = f2.read()

      options, categories = systrace.parse_options([])
      agent = atrace_agent.AtraceAgent(options, categories)
      trace_data = agent._preprocess_trace_data(atrace_data_with_thread_list)
      self.assertEqual(atrace_data, trace_data)

  def test_list_categories(self):
    options, categories = systrace.parse_options(SYSTRACE_LIST_CATEGORIES_CMD)
    agent = atrace_agent.AtraceAgent(options, categories)
    tracer_args = agent._construct_trace_command()
    self.assertEqual(' '.join(TRACE_LIST_CATEGORIES_CMD), ' '.join(tracer_args))
    self.assertEqual(False, agent.expect_trace())

class AtraceLegacyAgentUnitTest(unittest.TestCase):
  def test_construct_trace_command(self):
    options, categories = systrace.parse_options(SYSTRACE_CMD)
    agent = atrace_agent.AtraceLegacyAgent(options, categories)
    tracer_args = agent._construct_trace_command()
    self.assertEqual(' '.join(LEGACY_TRACE_CMD), ' '.join(tracer_args))
    self.assertEqual(True, agent.expect_trace())

if __name__ == '__main__':
    unittest.main()
