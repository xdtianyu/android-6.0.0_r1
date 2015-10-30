#!/usr/bin/env python

import codecs, httplib, json, optparse, os, urllib, shutil, subprocess, sys

output_html_file = 'systrace_trace_viewer.html'

upstream_git = 'https://github.com/google/trace-viewer.git'

script_dir = os.path.dirname(os.path.abspath(sys.argv[0]))
trace_viewer_dir = os.path.join(script_dir, 'trace-viewer')

parser = optparse.OptionParser()
parser.add_option('--local', dest='local_dir', metavar='DIR',
                  help='use a local trace-viewer')
parser.add_option('--no-min', dest='no_min', default=False, action='store_true',
                  help='skip minification')
options, args = parser.parse_args()

# Update the source if needed.
if options.local_dir is None:
  # Remove the old source tree.
  shutil.rmtree(trace_viewer_dir, True)

  # Pull the latest source from the upstream git.
  git_args = ['git', 'clone', upstream_git, trace_viewer_dir]
  p = subprocess.Popen(git_args, stdout=subprocess.PIPE, cwd=script_dir)
  p.communicate()
  if p.wait() != 0:
    print 'Failed to checkout source from upstream git.'
    sys.exit(1)

  trace_viewer_git_dir = os.path.join(trace_viewer_dir, '.git')
  # Update the UPSTREAM_REVISION file
  git_args = ['git', 'rev-parse', 'HEAD']
  p = subprocess.Popen(git_args,
                       stdout=subprocess.PIPE,
                       cwd=trace_viewer_dir,
                       env={"GIT_DIR":trace_viewer_git_dir})
  out, err = p.communicate()
  if p.wait() != 0:
    print 'Failed to get revision.'
    sys.exit(1)

  shutil.rmtree(trace_viewer_git_dir, True)

  rev = out.strip()
  with open('UPSTREAM_REVISION', 'wt') as f:
    f.write(rev + '\n')
else:
  trace_viewer_dir = options.local_dir


# Generate the vulcanized result.
build_dir = os.path.join(trace_viewer_dir)
sys.path.append(build_dir)

from tracing.build import vulcanize_trace_viewer
with codecs.open(output_html_file, encoding='utf-8', mode='w') as f:
  vulcanize_trace_viewer.WriteTraceViewer(
      f,
      config_name='systrace',
      minify=(not options.no_min),
      output_html_head_and_body=False)
print 'Generated %s' % output_html_file
