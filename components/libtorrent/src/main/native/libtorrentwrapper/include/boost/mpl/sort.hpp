
#ifndef BOOST_MPL_SORT_HPP_INCLUDED
#define BOOST_MPL_SORT_HPP_INCLUDED

// Copyright Eric Friedman 2002-2003
// Copyright Aleksey Gurtovoy 2004
//
// Distributed under the Boost Software License, Version 1.0. 
// (See accompanying file LICENSE_1_0.txt or copy at 
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/mpl for documentation.

// $Id: sort.hpp,v 1.2 2009-05-21 17:03:42 pvertenten Exp $
// $Date: 2009-05-21 17:03:42 $
// $Revision: 1.2 $

#include <boost/mpl/aux_/sort_impl.hpp>
#include <boost/mpl/aux_/inserter_algorithm.hpp>

namespace boost { namespace mpl {

BOOST_MPL_AUX_INSERTER_ALGORITHM_DEF(3, sort)

}}

#endif // BOOST_MPL_SORT_HPP_INCLUDED
